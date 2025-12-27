package com.trabix.document.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trabix.common.exception.RecursoNoEncontradoException;
import com.trabix.common.exception.ValidacionNegocioException;
import com.trabix.document.dto.DocumentoDTO;
import com.trabix.document.entity.Documento;
import com.trabix.document.entity.EstadoDocumento;
import com.trabix.document.entity.TipoDocumento;
import com.trabix.document.entity.Usuario;
import com.trabix.document.repository.DocumentoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio para gestión de documentos (cotizaciones y facturas).
 * 
 * Solo el ADMIN puede crear y gestionar documentos.
 * Items son solo TRABIX (granizados).
 * NO se puede anular documento PAGADO.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentoService {

    private final DocumentoRepository repository;
    private final ObjectMapper objectMapper;

    @Value("${trabix.documentos.iva-porcentaje:19}")
    private int ivaPorcentaje;

    @Value("${trabix.documentos.dias-vencimiento-cotizacion:15}")
    private int diasVencimientoCotizacion;

    // ==================== CREAR ====================

    @Transactional
    public DocumentoDTO.Response crear(DocumentoDTO.CreateRequest request, Usuario usuario) {
        // Calcular totales
        List<DocumentoDTO.ItemDocumento> items = calcularSubtotalesItems(request.getItems());
        BigDecimal subtotal = calcularSubtotal(items);
        BigDecimal iva = BigDecimal.ZERO;
        
        if (Boolean.TRUE.equals(request.getAplicarIva())) {
            iva = subtotal.multiply(BigDecimal.valueOf(ivaPorcentaje))
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        }
        
        BigDecimal total = subtotal.add(iva);

        // Fecha de vencimiento para cotizaciones
        LocalDateTime fechaVencimiento = null;
        if (TipoDocumento.COTIZACION.equals(request.getTipo())) {
            int dias = request.getDiasVencimiento() != null ? 
                    request.getDiasVencimiento() : diasVencimientoCotizacion;
            fechaVencimiento = LocalDateTime.now().plusDays(dias);
        }

        Documento documento = Documento.builder()
                .tipo(request.getTipo())
                .usuario(usuario)
                .clienteNombre(request.getClienteNombre().trim())
                .clienteTelefono(request.getClienteTelefono())
                .clienteDireccion(request.getClienteDireccion())
                .clienteNit(request.getClienteNit())
                .clienteCorreo(request.getClienteCorreo())
                .items(serializarItems(items))
                .subtotal(subtotal)
                .iva(iva)
                .total(total)
                .fechaEmision(LocalDateTime.now())
                .fechaVencimiento(fechaVencimiento)
                .estado(EstadoDocumento.BORRADOR)
                .notas(request.getNotas())
                .build();

        Documento saved = repository.save(documento);
        log.info("{} creada: ID={} - Cliente: {} - Total: ${}",
                saved.getTipo().getDescripcion(), saved.getId(), 
                saved.getClienteNombre(), saved.getTotal());

        return mapToResponse(saved);
    }

    // ==================== ACTUALIZAR ====================

    @Transactional
    public DocumentoDTO.Response actualizar(Long id, DocumentoDTO.UpdateRequest request) {
        Documento documento = repository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Documento", id));

        if (!documento.puedeEditarse()) {
            throw new ValidacionNegocioException(
                    "Solo se pueden editar documentos en estado BORRADOR. Estado actual: " + 
                    documento.getEstado().getNombre());
        }

        if (request.getClienteNombre() != null) {
            documento.setClienteNombre(request.getClienteNombre().trim());
        }
        if (request.getClienteTelefono() != null) {
            documento.setClienteTelefono(request.getClienteTelefono());
        }
        if (request.getClienteDireccion() != null) {
            documento.setClienteDireccion(request.getClienteDireccion());
        }
        if (request.getClienteNit() != null) {
            documento.setClienteNit(request.getClienteNit());
        }
        if (request.getClienteCorreo() != null) {
            documento.setClienteCorreo(request.getClienteCorreo());
        }
        if (request.getNotas() != null) {
            documento.setNotas(request.getNotas());
        }

        // Recalcular si hay nuevos items
        if (request.getItems() != null && !request.getItems().isEmpty()) {
            List<DocumentoDTO.ItemDocumento> items = calcularSubtotalesItems(request.getItems());
            BigDecimal subtotal = calcularSubtotal(items);
            BigDecimal iva = BigDecimal.ZERO;
            
            if (Boolean.TRUE.equals(request.getAplicarIva())) {
                iva = subtotal.multiply(BigDecimal.valueOf(ivaPorcentaje))
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            }
            
            documento.setItems(serializarItems(items));
            documento.setSubtotal(subtotal);
            documento.setIva(iva);
            documento.setTotal(subtotal.add(iva));
        }

        Documento saved = repository.save(documento);
        log.info("{} actualizada: ID={}", saved.getTipo().getDescripcion(), saved.getId());

        return mapToResponse(saved);
    }

    // ==================== EMITIR ====================

    @Transactional
    public DocumentoDTO.Response emitir(Long id) {
        Documento documento = repository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Documento", id));

        if (!documento.puedeEmitirse()) {
            throw new ValidacionNegocioException(
                    "Solo se pueden emitir documentos en estado BORRADOR. Estado actual: " + 
                    documento.getEstado().getNombre());
        }

        // Generar número con bloqueo para evitar duplicados
        String numero = generarNumero(documento.getTipo());
        documento.emitir(numero);

        Documento saved = repository.save(documento);
        log.info("{} emitida: {} - Cliente: {} - Total: ${}",
                saved.getTipo().getDescripcion(), saved.getNumero(), 
                saved.getClienteNombre(), saved.getTotal());

        return mapToResponse(saved);
    }

    // ==================== MARCAR PAGADO ====================

    @Transactional
    public DocumentoDTO.Response marcarPagado(Long id) {
        Documento documento = repository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Documento", id));

        if (!documento.puedePagarse()) {
            throw new ValidacionNegocioException(
                    "Solo se pueden marcar como pagados documentos EMITIDOS. Estado actual: " + 
                    documento.getEstado().getNombre());
        }

        documento.marcarPagado();
        Documento saved = repository.save(documento);
        log.info("{} pagada: {} - Total: ${}", 
                saved.getTipo().getDescripcion(), saved.getNumero(), saved.getTotal());

        return mapToResponse(saved);
    }

    // ==================== ANULAR ====================

    @Transactional
    public DocumentoDTO.Response anular(Long id) {
        Documento documento = repository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Documento", id));

        if (!documento.puedeAnularse()) {
            throw new ValidacionNegocioException(
                    "No se puede anular el documento. Estado actual: " + 
                    documento.getEstado().getNombre() + 
                    ". Nota: Los documentos PAGADOS no pueden anularse.");
        }

        documento.anular();
        Documento saved = repository.save(documento);
        log.info("{} anulada: {}", 
                saved.getTipo().getDescripcion(), 
                saved.tieneNumero() ? saved.getNumero() : "ID=" + saved.getId());

        return mapToResponse(saved);
    }

    // ==================== CONVERTIR A FACTURA ====================

    @Transactional
    public DocumentoDTO.Response convertirAFactura(Long cotizacionId, DocumentoDTO.ConvertirAFacturaRequest request) {
        Documento cotizacion = repository.findById(cotizacionId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cotización", cotizacionId));

        if (!cotizacion.esCotizacion()) {
            throw new ValidacionNegocioException("Solo se pueden convertir COTIZACIONES a facturas");
        }

        if (!cotizacion.estaEmitido() && !cotizacion.estaPagado()) {
            throw new ValidacionNegocioException(
                    "La cotización debe estar EMITIDA o PAGADA para convertirse en factura. " +
                    "Estado actual: " + cotizacion.getEstado().getNombre());
        }

        // Verificar si ya existe una factura de esta cotización
        if (repository.existsByCotizacionOrigenId(cotizacionId)) {
            throw new ValidacionNegocioException(
                    "Ya existe una factura generada a partir de esta cotización");
        }

        // Crear factura basada en la cotización
        Documento factura = Documento.builder()
                .tipo(TipoDocumento.FACTURA)
                .usuario(cotizacion.getUsuario())
                .clienteNombre(cotizacion.getClienteNombre())
                .clienteTelefono(cotizacion.getClienteTelefono())
                .clienteDireccion(cotizacion.getClienteDireccion())
                .clienteNit(request != null && request.getClienteNit() != null ? 
                        request.getClienteNit() : cotizacion.getClienteNit())
                .clienteCorreo(request != null && request.getClienteCorreo() != null ? 
                        request.getClienteCorreo() : cotizacion.getClienteCorreo())
                .items(cotizacion.getItems())
                .subtotal(cotizacion.getSubtotal())
                .iva(cotizacion.getIva())
                .total(cotizacion.getTotal())
                .fechaEmision(LocalDateTime.now())
                .estado(EstadoDocumento.BORRADOR)
                .notas(request != null && request.getNotas() != null ? 
                        request.getNotas() : cotizacion.getNotas())
                .cotizacionOrigenId(cotizacionId)
                .build();

        Documento saved = repository.save(factura);
        log.info("Cotización {} convertida a Factura ID={}", 
                cotizacion.getNumero(), saved.getId());

        return mapToResponse(saved);
    }

    // ==================== CONSULTAS ====================

    @Transactional(readOnly = true)
    public DocumentoDTO.Response obtener(Long id) {
        Documento documento = repository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Documento", id));
        return mapToResponse(documento);
    }

    @Transactional(readOnly = true)
    public DocumentoDTO.Response obtenerPorNumero(String numero) {
        Documento documento = repository.findByNumero(numero)
                .orElseThrow(() -> new RecursoNoEncontradoException("Documento", numero));
        return mapToResponse(documento);
    }

    @Transactional(readOnly = true)
    public DocumentoDTO.ListResponse listar(Pageable pageable) {
        Page<Documento> page = repository.findAll(pageable);
        return buildListResponse(page);
    }

    @Transactional(readOnly = true)
    public DocumentoDTO.ListResponse listarPorTipo(TipoDocumento tipo, Pageable pageable) {
        Page<Documento> page = repository.findByTipo(tipo, pageable);
        return buildListResponse(page);
    }

    @Transactional(readOnly = true)
    public DocumentoDTO.ListResponse listarPorTipoYEstado(
            TipoDocumento tipo, EstadoDocumento estado, Pageable pageable) {
        Page<Documento> page = repository.findByTipoAndEstado(tipo, estado, pageable);
        return buildListResponse(page);
    }

    @Transactional(readOnly = true)
    public DocumentoDTO.ListResponse listarPorUsuario(Long usuarioId, Pageable pageable) {
        Page<Documento> page = repository.findByUsuarioId(usuarioId, pageable);
        return buildListResponse(page);
    }

    @Transactional(readOnly = true)
    public List<DocumentoDTO.Response> buscarPorCliente(String nombre) {
        return repository.buscarPorCliente(nombre).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<DocumentoDTO.Response> listarRecientes(TipoDocumento tipo) {
        return repository.findTop10ByTipoOrderByFechaEmisionDesc(tipo).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<DocumentoDTO.Response> listarCotizacionesVencidas() {
        return repository.findCotizacionesVencidas(LocalDateTime.now()).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ==================== RESUMEN ====================

    @Transactional(readOnly = true)
    public DocumentoDTO.ResumenDocumentos obtenerResumen(TipoDocumento tipo) {
        return DocumentoDTO.ResumenDocumentos.builder()
                .tipo(tipo)
                .tipoDescripcion(tipo.getDescripcion())
                .total(repository.countByTipo(tipo))
                .borradores(repository.countByTipoAndEstado(tipo, EstadoDocumento.BORRADOR))
                .emitidos(repository.countByTipoAndEstado(tipo, EstadoDocumento.EMITIDO))
                .pagados(repository.countByTipoAndEstado(tipo, EstadoDocumento.PAGADO))
                .anulados(repository.countByTipoAndEstado(tipo, EstadoDocumento.ANULADO))
                .vencidos(repository.countByTipoAndEstado(tipo, EstadoDocumento.VENCIDO))
                .totalPagado(repository.sumarTotalPagadoPorTipo(tipo))
                .totalPendiente(repository.sumarTotalPendientePorTipo(tipo))
                .build();
    }

    // ==================== TAREA PROGRAMADA ====================

    /**
     * Marca cotizaciones vencidas automáticamente.
     * Se ejecuta cada hora.
     */
    @Scheduled(fixedRate = 3600000) // cada hora
    @Transactional
    public void marcarCotizacionesVencidas() {
        int actualizados = repository.marcarCotizacionesVencidas(LocalDateTime.now());
        if (actualizados > 0) {
            log.info("Cotizaciones marcadas como vencidas: {}", actualizados);
        }
    }

    // ==================== MÉTODOS PRIVADOS ====================

    private String generarNumero(TipoDocumento tipo) {
        String prefijo = tipo.getPrefijo();
        int anio = LocalDateTime.now().getYear();
        
        Integer maxConsecutivo = repository.findMaxConsecutivoByTipoYAnio(tipo, prefijo, anio)
                .orElse(0);
        int consecutivo = maxConsecutivo + 1;
        
        return String.format("%s-%d-%05d", prefijo, anio, consecutivo);
    }

    private List<DocumentoDTO.ItemDocumento> calcularSubtotalesItems(List<DocumentoDTO.ItemDocumento> items) {
        return items.stream().map(item -> {
            BigDecimal subtotal = item.getPrecioUnitario()
                    .multiply(BigDecimal.valueOf(item.getCantidad()));
            return DocumentoDTO.ItemDocumento.builder()
                    .descripcion(item.getDescripcion())
                    .cantidad(item.getCantidad())
                    .precioUnitario(item.getPrecioUnitario())
                    .subtotal(subtotal)
                    .build();
        }).collect(Collectors.toList());
    }

    private BigDecimal calcularSubtotal(List<DocumentoDTO.ItemDocumento> items) {
        return items.stream()
                .map(DocumentoDTO.ItemDocumento::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String serializarItems(List<DocumentoDTO.ItemDocumento> items) {
        try {
            return objectMapper.writeValueAsString(items);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error al serializar items", e);
        }
    }

    private List<DocumentoDTO.ItemDocumento> deserializarItems(String json) {
        try {
            if (json == null || json.isBlank()) {
                return new ArrayList<>();
            }
            return objectMapper.readValue(json, new TypeReference<List<DocumentoDTO.ItemDocumento>>() {});
        } catch (JsonProcessingException e) {
            log.error("Error al deserializar items: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    private DocumentoDTO.ListResponse buildListResponse(Page<Documento> page) {
        List<DocumentoDTO.Response> documentos = page.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return DocumentoDTO.ListResponse.builder()
                .documentos(documentos)
                .pagina(page.getNumber())
                .tamano(page.getSize())
                .totalElementos(page.getTotalElements())
                .totalPaginas(page.getTotalPages())
                .build();
    }

    private DocumentoDTO.Response mapToResponse(Documento doc) {
        // Obtener número de cotización origen si existe
        String cotizacionOrigenNumero = null;
        if (doc.getCotizacionOrigenId() != null) {
            cotizacionOrigenNumero = repository.findById(doc.getCotizacionOrigenId())
                    .map(Documento::getNumero)
                    .orElse(null);
        }

        return DocumentoDTO.Response.builder()
                .id(doc.getId())
                .tipo(doc.getTipo())
                .tipoDescripcion(doc.getTipo().getDescripcion())
                .numero(doc.getNumero())
                .usuarioId(doc.getUsuario() != null ? doc.getUsuario().getId() : null)
                .usuarioNombre(doc.getUsuario() != null ? doc.getUsuario().getNombre() : null)
                .clienteNombre(doc.getClienteNombre())
                .clienteTelefono(doc.getClienteTelefono())
                .clienteDireccion(doc.getClienteDireccion())
                .clienteNit(doc.getClienteNit())
                .clienteCorreo(doc.getClienteCorreo())
                .items(deserializarItems(doc.getItems()))
                .subtotal(doc.getSubtotal())
                .iva(doc.getIva())
                .total(doc.getTotal())
                .fechaEmision(doc.getFechaEmision())
                .fechaVencimiento(doc.getFechaVencimiento())
                .estado(doc.getEstado())
                .estadoDescripcion(doc.getEstado().getNombre())
                .notas(doc.getNotas())
                .cotizacionOrigenId(doc.getCotizacionOrigenId())
                .cotizacionOrigenNumero(cotizacionOrigenNumero)
                .createdAt(doc.getCreatedAt())
                // Flags de acciones
                .puedeEditarse(doc.puedeEditarse())
                .puedeEmitirse(doc.puedeEmitirse())
                .puedePagarse(doc.puedePagarse())
                .puedeAnularse(doc.puedeAnularse())
                .puedeConvertirse(doc.esCotizacion() && doc.estaEmitido())
                .build();
    }
}
