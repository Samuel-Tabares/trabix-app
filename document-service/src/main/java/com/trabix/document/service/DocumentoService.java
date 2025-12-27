package com.trabix.document.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trabix.common.exception.RecursoNoEncontradoException;
import com.trabix.common.exception.ValidacionNegocioException;
import com.trabix.document.dto.DocumentoDTO;
import com.trabix.document.entity.Documento;
import com.trabix.document.entity.Usuario;
import com.trabix.document.repository.DocumentoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    @Value("${trabix.documentos.prefijo-cotizacion:COT}")
    private String prefijoCotizacion;

    @Value("${trabix.documentos.prefijo-factura:FAC}")
    private String prefijoFactura;

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
        if ("COTIZACION".equals(request.getTipo())) {
            int dias = request.getDiasVencimiento() != null ? 
                    request.getDiasVencimiento() : diasVencimientoCotizacion;
            fechaVencimiento = LocalDateTime.now().plusDays(dias);
        }

        Documento documento = Documento.builder()
                .tipo(request.getTipo())
                .usuario(usuario)
                .clienteNombre(request.getClienteNombre())
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
                .estado("BORRADOR")
                .notas(request.getNotas())
                .build();

        Documento saved = repository.save(documento);
        log.info("{} creada: ID={} - Cliente: {} - Total: ${}",
                saved.getTipo(), saved.getId(), saved.getClienteNombre(), saved.getTotal());

        return mapToResponse(saved);
    }

    @Transactional
    public DocumentoDTO.Response actualizar(Long id, DocumentoDTO.UpdateRequest request) {
        Documento documento = repository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Documento", id));

        if (!documento.esBorrador()) {
            throw new ValidacionNegocioException("Solo se pueden editar documentos en estado BORRADOR");
        }

        if (request.getClienteNombre() != null) {
            documento.setClienteNombre(request.getClienteNombre());
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
        log.info("{} actualizada: ID={}", saved.getTipo(), saved.getId());

        return mapToResponse(saved);
    }

    @Transactional
    public DocumentoDTO.Response emitir(Long id) {
        Documento documento = repository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Documento", id));

        if (!documento.esBorrador()) {
            throw new ValidacionNegocioException("Solo se pueden emitir documentos en estado BORRADOR");
        }

        // Generar número
        String numero = generarNumero(documento.getTipo());
        documento.setNumero(numero);
        documento.emitir();

        Documento saved = repository.save(documento);
        log.info("{} emitida: {} - Cliente: {} - Total: ${}",
                saved.getTipo(), saved.getNumero(), saved.getClienteNombre(), saved.getTotal());

        return mapToResponse(saved);
    }

    @Transactional
    public DocumentoDTO.Response marcarPagado(Long id) {
        Documento documento = repository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Documento", id));

        if (!documento.estaEmitido()) {
            throw new ValidacionNegocioException("Solo se pueden marcar como pagados documentos EMITIDOS");
        }

        documento.marcarPagado();
        Documento saved = repository.save(documento);
        log.info("{} pagada: {} - Total: ${}", saved.getTipo(), saved.getNumero(), saved.getTotal());

        return mapToResponse(saved);
    }

    @Transactional
    public DocumentoDTO.Response anular(Long id) {
        Documento documento = repository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Documento", id));

        if (documento.estaAnulado()) {
            throw new ValidacionNegocioException("El documento ya está anulado");
        }

        documento.anular();
        Documento saved = repository.save(documento);
        log.info("{} anulada: {}", saved.getTipo(), saved.getNumero() != null ? saved.getNumero() : "ID=" + saved.getId());

        return mapToResponse(saved);
    }

    @Transactional
    public DocumentoDTO.Response convertirAFactura(Long cotizacionId, DocumentoDTO.ConvertirAFacturaRequest request) {
        Documento cotizacion = repository.findById(cotizacionId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cotización", cotizacionId));

        if (!cotizacion.esCotizacion()) {
            throw new ValidacionNegocioException("Solo se pueden convertir cotizaciones a facturas");
        }

        if (!cotizacion.estaEmitido()) {
            throw new ValidacionNegocioException("La cotización debe estar emitida para convertirse en factura");
        }

        // Crear factura basada en la cotización
        Documento factura = Documento.builder()
                .tipo("FACTURA")
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
                .estado("BORRADOR")
                .notas(request != null && request.getNotas() != null ? 
                        request.getNotas() : cotizacion.getNotas())
                .cotizacionOrigenId(cotizacionId)
                .build();

        Documento saved = repository.save(factura);
        log.info("Cotización {} convertida a Factura ID={}", 
                cotizacion.getNumero(), saved.getId());

        return mapToResponse(saved);
    }

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
    public DocumentoDTO.ListResponse listarPorTipo(String tipo, Pageable pageable) {
        Page<Documento> page = repository.findByTipo(tipo.toUpperCase(), pageable);
        return buildListResponse(page);
    }

    @Transactional(readOnly = true)
    public DocumentoDTO.ListResponse listarPorTipoYEstado(String tipo, String estado, Pageable pageable) {
        Page<Documento> page = repository.findByTipoAndEstado(tipo.toUpperCase(), estado.toUpperCase(), pageable);
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
    public List<DocumentoDTO.Response> listarRecientes(String tipo) {
        return repository.findTop10ByTipoOrderByFechaEmisionDesc(tipo.toUpperCase()).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public DocumentoDTO.ResumenDocumentos obtenerResumen(String tipo) {
        String tipoUpper = tipo.toUpperCase();
        
        return DocumentoDTO.ResumenDocumentos.builder()
                .tipo(tipoUpper)
                .total(repository.countByTipo(tipoUpper))
                .borradores(repository.countByTipoAndEstado(tipoUpper, "BORRADOR"))
                .emitidos(repository.countByTipoAndEstado(tipoUpper, "EMITIDO"))
                .pagados(repository.countByTipoAndEstado(tipoUpper, "PAGADO"))
                .anulados(repository.countByTipoAndEstado(tipoUpper, "ANULADO"))
                .totalPagado(repository.sumarTotalPagadoPorTipo(tipoUpper))
                .totalPendiente(repository.sumarTotalPendientePorTipo(tipoUpper))
                .build();
    }

    private String generarNumero(String tipo) {
        String prefijo = "COTIZACION".equals(tipo) ? prefijoCotizacion : prefijoFactura;
        int anio = LocalDateTime.now().getYear();
        
        Long maxId = repository.findMaxIdByTipoYAnio(tipo, anio).orElse(0L);
        long consecutivo = maxId + 1;
        
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
        return DocumentoDTO.Response.builder()
                .id(doc.getId())
                .tipo(doc.getTipo())
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
                .notas(doc.getNotas())
                .cotizacionOrigenId(doc.getCotizacionOrigenId())
                .createdAt(doc.getCreatedAt())
                .build();
    }
}
