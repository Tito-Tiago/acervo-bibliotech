package com.bibliotech.bibliotech.controllers;

import com.bibliotech.bibliotech.dtos.request.AlunoRequestDTO;
import com.bibliotech.bibliotech.dtos.response.AlunoResponseDTO;
import com.bibliotech.bibliotech.dtos.response.mappers.AlunoResponseMapper;
import com.bibliotech.bibliotech.models.Aluno;
import com.bibliotech.bibliotech.services.AlunosService;
import com.bibliotech.bibliotech.services.PdfExportService;
import org.springframework.http.HttpHeaders;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.stream.Collectors;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/alunos")
public class AlunoController {

    private final AlunosService alunosService;
    private final PdfExportService pdfExportService;

    public AlunoController(AlunosService alunosService, PdfExportService pdfExportService) {
        this.alunosService = alunosService;
        this.pdfExportService = pdfExportService;
    }

    @GetMapping("")
    public ResponseEntity<Page<AlunoResponseDTO>> listarAlunos(
            @RequestParam(value = "serie", required = false) Integer serie,
            @RequestParam(value = "turma", required = false) String turma,
            @RequestParam(value = "nome", required = false) String nome,
            @RequestParam(value = "situacao", required = false) String situacao,
            @RequestParam(value = "ativo", required = false) Boolean ativo,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size)
            {


        Pageable pageable = PageRequest.of(page, size);

        Page<Aluno> alunosPage = alunosService.filtrarAlunos(serie, turma, nome, situacao, ativo, pageable);

        // convert Page<Aluno> to Page<AlunoResponseDTO>
        Page<AlunoResponseDTO> alunosResponseDTOPage = new PageImpl<>(
                alunosPage.getContent().stream()
                        .map(AlunoResponseMapper::toDto)
                        .collect(Collectors.toList()),
                alunosPage.getPageable(),
                alunosPage.getTotalElements()
        );

        return ResponseEntity.ok(alunosResponseDTOPage);
    }
    @GetMapping("/{id}")
    public ResponseEntity<AlunoResponseDTO> buscarAlunoPorId(@PathVariable Integer id) {
        return ResponseEntity.ok(AlunoResponseMapper.toDto(alunosService.buscarAlunoPorId(id)));
    }

    @PostMapping
    public ResponseEntity<AlunoResponseDTO> cadastrarAluno(@RequestBody AlunoRequestDTO requestDTO) {
        return ResponseEntity.status(HttpStatus.CREATED).body(AlunoResponseMapper.toDto(alunosService.cadastrarAluno(requestDTO)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AlunoResponseDTO> atualizarAluno(
            @PathVariable Integer id,
            @RequestBody AlunoRequestDTO requestDTO) {
        return ResponseEntity.ok(AlunoResponseMapper.toDto(alunosService.atualizarAluno(id, requestDTO)));
    }

    @PatchMapping("/inativar/{id}")
    public ResponseEntity<Void> inativarAluno(@PathVariable Integer id) {
        alunosService.inativarAluno(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/ativar/{id}")
    public ResponseEntity<Void> ativarAluno(@PathVariable Integer id) {
        alunosService.ativarAluno(id);
        return ResponseEntity.noContent().build();
    }

    //coloquei a data de inicio como nao obrigatorio para tratar dela bonitinho no service
    @GetMapping("/mais-leitores/export/pdf")
    public ResponseEntity<byte[]> exportTopLeitoresPdf(@RequestParam(required = false) LocalDate dataInicio, @RequestParam(required = false) LocalDate dataFim, @RequestParam(required = false) Integer qtdMax) {
        byte[] pdfBytes = pdfExportService.exportAlunosMaisLeitores(alunosService.obterAlunosMaisLeitures(dataInicio, dataFim, qtdMax));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "top-leitores.pdf");

        return ResponseEntity.ok()
                .headers(headers)
                .body(pdfBytes);
    }
}