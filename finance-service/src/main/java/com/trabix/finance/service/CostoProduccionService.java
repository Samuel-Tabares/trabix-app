package com.trabix.finance.service;

import com.trabix.common.exception.RecursoNoEncontradoException;
import com.trabix.finance.dto.CostoProduccionDTO;
import com.trabix.finance.entity.CostoProduccion;
import com.trabix.finance.entity.TipoCosto;
import com.trabix.finance.repository.CostoProduccionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
 * Servicio para gestión de costos de producción.
 * El ADMIN registra todos los gastos manualmente.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CostoProduccionService {

    private final CostoProduccionRepository repository;

    @Transactional
    public CostoProduccionDTO.Response crear(CostoProduccionDTO.CreateRequest request) {
        BigDecimal costoTotal = request.getCostoUnitario()
                .multiply(BigDecimal.valueOf(request.getCantidad()));

        CostoProduccion costo = CostoProduccion.builder()
                .concepto(request.getConcepto().trim())
                .cantidad(request.getCantidad())
                .costoUnitario(request.getCostoUnitario())
                .costoTotal(costoTotal)
                .tipo(request.getTipo())
                .fecha(request.getFecha() != null ? request.getFecha() : LocalDateTime.now())
                .nota(request.getNota())
                .proveedor(request.getProveedor() != null ? request.getProveedor().trim() : null)
                .numeroFactura(request.getNumeroFactura() != null ? request.getNumeroFactura().trim() : null)
                .build();

        CostoProduccion saved = repository.save(costo);
        
        log.info("Costo registrado: {} - {} x ${} = ${} ({})",
                saved.getConcepto(), saved.getCantidad(), saved.getCostoUnitario(),
                saved.getCostoTotal(), saved.getTipo());

        return mapToResponse(saved);
    }

    @Transactional
    public CostoProduccionDTO.Response actualizar(Long id, CostoProduccionDTO.UpdateRequest request) {
        CostoProduccion costo = repository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Costo", id));

        boolean recalcular = false;

        if (request.getConcepto() != null) {
            costo.setConcepto(request.getConcepto().trim());
        }
        if (request.getCantidad() != null) {
            costo.setCantidad(request.getCantidad());
            recalcular = true;
        }
        if (request.getCostoUnitario() != null) {
            costo.setCostoUnitario(request.getCostoUnitario());
            recalcular = true;
        }
        if (request.getTipo() != null) {
            costo.setTipo(request.getTipo());
        }
        if (request.getNota() != null) {
            costo.setNota(request.getNota());
        }
        if (request.getProveedor() != null) {
            costo.setProveedor(request.getProveedor().trim());
        }
        if (request.getNumeroFactura() != null) {
            costo.setNumeroFactura(request.getNumeroFactura().trim());
        }

        if (recalcular) {
            costo.recalcularTotal();
        }

        CostoProduccion saved = repository.save(costo);
        log.info("Costo actualizado: {} (ID: {})", saved.getConcepto(), saved.getId());

        return mapToResponse(saved);
    }

    @Transactional
    public void eliminar(Long id) {
        CostoProduccion costo = repository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Costo", id));
        repository.delete(costo);
        log.info("Costo eliminado: {} - ${} (ID: {})", costo.getConcepto(), costo.getCostoTotal(), id);
    }

    @Transactional(readOnly = true)
    public CostoProduccionDTO.Response obtener(Long id) {
        CostoProduccion costo = repository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Costo", id));
        return mapToResponse(costo);
    }

    @Transactional(readOnly = true)
    public CostoProduccionDTO.ListResponse listar(Pageable pageable) {
        Page<CostoProduccion> page = repository.findAll(pageable);
        BigDecimal totalCostos = repository.sumarTotalCostos();
        return buildListResponse(page, totalCostos);
    }

    @Transactional(readOnly = true)
    public CostoProduccionDTO.ListResponse listarPorTipo(TipoCosto tipo, Pageable pageable) {
        Page<CostoProduccion> page = repository.findByTipo(tipo, pageable);
        BigDecimal totalCostos = repository.sumarCostosPorTipo(tipo);
        return buildListResponse(page, totalCostos);
    }

    @Transactional(readOnly = true)
    public CostoProduccionDTO.ListResponse listarPorPeriodo(LocalDateTime desde, LocalDateTime hasta, Pageable pageable) {
        Page<CostoProduccion> page = repository.findByFechaBetween(desde, hasta, pageable);
        BigDecimal totalCostos = repository.sumarCostosPeriodo(desde, hasta);
        return buildListResponse(page, totalCostos);
    }

    @Transactional(readOnly = true)
    public List<CostoProduccionDTO.Response> buscarPorConcepto(String concepto) {
        return repository.buscarPorConcepto(concepto).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CostoProduccionDTO.Response> listarUltimos() {
        return repository.findTop10ByOrderByFechaDesc().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CostoProduccionDTO.ResumenGeneral obtenerResumenGeneral() {
        return buildResumen(null, null);
    }

    @Transactional(readOnly = true)
    public CostoProduccionDTO.ResumenGeneral obtenerResumenPeriodo(LocalDateTime desde, LocalDateTime hasta) {
        return buildResumen(desde, hasta);
    }

    @Transactional(readOnly = true)
    public List<String> listarProveedores() {
        return repository.listarProveedores();
    }

    private CostoProduccionDTO.ListResponse buildListResponse(Page<CostoProduccion> page, BigDecimal totalCostos) {
        List<CostoProduccionDTO.Response> costos = page.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return CostoProduccionDTO.ListResponse.builder()
                .costos(costos)
                .pagina(page.getNumber())
                .tamano(page.getSize())
                .totalElementos(page.getTotalElements())
                .totalPaginas(page.getTotalPages())
                .totalCostos(totalCostos)
                .build();
    }

    private CostoProduccionDTO.ResumenGeneral buildResumen(LocalDateTime desde, LocalDateTime hasta) {
        List<Object[]> resumenData;
        BigDecimal totalGeneral;
        long totalRegistros;

        if (desde != null && hasta != null) {
            resumenData = repository.obtenerResumenPorTipoPeriodo(desde, hasta);
            totalGeneral = repository.sumarCostosPeriodo(desde, hasta);
            totalRegistros = repository.countByFechaBetween(desde, hasta);
        } else {
            resumenData = repository.obtenerResumenPorTipo();
            totalGeneral = repository.sumarTotalCostos();
            totalRegistros = repository.count();
        }

        List<CostoProduccionDTO.ResumenTipo> porTipo = new ArrayList<>();
        for (Object[] row : resumenData) {
            TipoCosto tipo = (TipoCosto) row[0];
            Long cantidad = (Long) row[1];
            BigDecimal total = (BigDecimal) row[2];
            
            BigDecimal porcentaje = BigDecimal.ZERO;
            if (totalGeneral.compareTo(BigDecimal.ZERO) > 0) {
                porcentaje = total.multiply(BigDecimal.valueOf(100))
                        .divide(totalGeneral, 2, RoundingMode.HALF_UP);
            }

            porTipo.add(CostoProduccionDTO.ResumenTipo.builder()
                    .tipo(tipo)
                    .tipoDescripcion(tipo.getDescripcion())
                    .cantidad(cantidad)
                    .total(total)
                    .porcentaje(porcentaje)
                    .build());
        }

        return CostoProduccionDTO.ResumenGeneral.builder()
                .totalGeneral(totalGeneral)
                .porTipo(porTipo)
                .desde(desde)
                .hasta(hasta)
                .totalRegistros(totalRegistros)
                .build();
    }

    private CostoProduccionDTO.Response mapToResponse(CostoProduccion costo) {
        return CostoProduccionDTO.Response.builder()
                .id(costo.getId())
                .concepto(costo.getConcepto())
                .cantidad(costo.getCantidad())
                .costoUnitario(costo.getCostoUnitario())
                .costoTotal(costo.getCostoTotal())
                .tipo(costo.getTipo())
                .tipoDescripcion(costo.getTipo().getDescripcion())
                .fecha(costo.getFecha())
                .nota(costo.getNota())
                .proveedor(costo.getProveedor())
                .numeroFactura(costo.getNumeroFactura())
                .createdAt(costo.getCreatedAt())
                .build();
    }
}
