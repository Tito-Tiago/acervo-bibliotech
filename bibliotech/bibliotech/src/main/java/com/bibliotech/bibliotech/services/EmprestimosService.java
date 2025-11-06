package com.bibliotech.bibliotech.services;

import com.bibliotech.bibliotech.dtos.request.EmprestimoRequestDTO;
import com.bibliotech.bibliotech.dtos.request.EmprestimoRequestDTOConcluir;
import com.bibliotech.bibliotech.dtos.response.EmprestimoNotificacaoDTO;
import com.bibliotech.bibliotech.dtos.response.EmprestimoResponseDTO;
import com.bibliotech.bibliotech.dtos.response.EmprestimoResponseDTOAluno;
import com.bibliotech.bibliotech.dtos.response.EmprestimoResponseDTOLivro;
import com.bibliotech.bibliotech.dtos.response.mappers.EmprestimoResponseMapper;
import com.bibliotech.bibliotech.facades.EmprestimoFacade;
import com.bibliotech.bibliotech.models.Emprestimo;
import com.bibliotech.bibliotech.repositories.EmprestimoRepository;
import com.bibliotech.bibliotech.specifications.EmprestimoSpecification;
import com.bibliotech.bibliotech.utils.EmailSend;
import com.bibliotech.bibliotech.utils.FormatarData;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Serviço de Empréstimos refatorado usando o padrão Facade.
 * O Facade encapsula as operações complexas, reduzindo acoplamento.
 */
@Service
public class EmprestimosService {

    private final EmprestimoFacade emprestimoFacade;
    private final EmprestimoRepository emprestimoRepository;
    private final EmprestimoResponseMapper emprestimoResponseMapper;
    private final EmprestimoSpecification emprestimoSpecification;
    private final EmailSend emailSend;
    private final NotificationService notificationService;

    @Autowired
    public EmprestimosService(
            EmprestimoFacade emprestimoFacade,
            EmprestimoRepository emprestimoRepository,
            EmprestimoResponseMapper emprestimoResponseMapper,
            EmprestimoSpecification emprestimoSpecification,
            EmailSend emailSend,
            NotificationService notificationService) {
        this.emprestimoFacade = emprestimoFacade;
        this.emprestimoRepository = emprestimoRepository;
        this.emprestimoResponseMapper = emprestimoResponseMapper;
        this.emprestimoSpecification = emprestimoSpecification;
        this.emailSend = emailSend;
        this.notificationService = notificationService;
    }

    // Métodos principais delegados ao Facade

    @Transactional
    public EmprestimoResponseDTO realizarEmprestimo(EmprestimoRequestDTO requestDTO) {
        return emprestimoFacade.realizarEmprestimo(requestDTO);
    }

    public String cancelarEmprestimo(Integer id) {
        return emprestimoFacade.cancelarEmprestimo(id);
    }

    @Transactional
    public String concluirEmprestimo(Integer id, EmprestimoRequestDTOConcluir DTOConcluir) {
        return emprestimoFacade.concluirEmprestimo(id, DTOConcluir);
    }

    @Transactional
    public String renovarPrazo(Integer id) {
        return emprestimoFacade.renovarPrazo(id);
    }

    // Métodos de consulta permanecem no service

    public Page<EmprestimoResponseDTO> consultarEmprestimos(
            String nomeAluno, String tituloLivro, String isbn, String situacao,
            String nomeRealizadoPor, LocalDate dataEmprestimo, String nomeConcluidoPor,
            LocalDate dataPrazo, LocalDate dataConclusao, Pageable pageable) {

        Specification<Emprestimo> spec = emprestimoSpecification.buildSpecification(
                nomeAluno, tituloLivro, isbn, situacao, nomeRealizadoPor,
                dataEmprestimo, nomeConcluidoPor, dataPrazo, dataConclusao);

        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "dataEmprestimo")
        );

        Page<Emprestimo> emprestimos = emprestimoRepository.findAll(spec, sortedPageable);

        return emprestimos.map(emprestimoResponseMapper::toDto);
    }

    public Page<EmprestimoResponseDTOAluno> consultarEmprestimosPorAlunoEPeriodo(
            Integer idAluno, LocalDate dataEmprestimoInicio, LocalDate dataEmprestimoFim, Pageable pageable) {

        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "dataEmprestimo")
        );

        Page<Emprestimo> emprestimos;

        if (dataEmprestimoInicio != null && dataEmprestimoFim != null) {
            emprestimos = emprestimoRepository.findByAlunoIdAndDataEmprestimoBetween(
                    idAluno, dataEmprestimoInicio, dataEmprestimoFim, sortedPageable);
        } else {
            emprestimos = emprestimoRepository.findByAlunoId(idAluno, sortedPageable);
        }

        return emprestimos.map(emprestimoResponseMapper::toDTOAluno);
    }

    public Page<EmprestimoResponseDTOLivro> consultarEmprestimosPorLivroEPeriodo(
            Integer idLivro, LocalDate dataEmprestimoInicio, LocalDate dataEmprestimoFim, Pageable pageable) {

        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "dataEmprestimo")
        );

        Page<Emprestimo> emprestimos;

        if (dataEmprestimoInicio != null && dataEmprestimoFim != null) {
            emprestimos = emprestimoRepository.findByExemplar_LivroIdAndDataEmprestimoBetween(
                    idLivro, dataEmprestimoInicio, dataEmprestimoFim, sortedPageable);
        } else {
            emprestimos = emprestimoRepository.findByExemplar_LivroId(idLivro, sortedPageable);
        }

        return emprestimos.map(emprestimoResponseMapper::toDTOLivro);
    }

    public List<EmprestimoNotificacaoDTO> enviarEmailAtrasadosEPresteAAtrasar() {
        LocalDate hoje = LocalDate.now();
        List<EmprestimoNotificacaoDTO> emprestimosNaoNotificados = new ArrayList<>();

        verificarAtrasados();

        List<Emprestimo> emprestimosAtrasados = emprestimoRepository.findBySituacao("atrasado");
        for (Emprestimo emprestimo : emprestimosAtrasados) {
            if (!enviarNotificacaoAtraso(emprestimo)) {
                emprestimosNaoNotificados.add(emprestimoResponseMapper.toDTONotificacao(emprestimo));
            }
        }

        LocalDate amanha = hoje.plusDays(1);
        List<Emprestimo> emprestimosPrestesAAtasar = emprestimoRepository.findBySituacaoAndDataPrazo("pendente", amanha);
        for (Emprestimo emprestimo : emprestimosPrestesAAtasar) {
            if (!enviarNotificacaoPreAtraso(emprestimo)) {
                emprestimosNaoNotificados.add(emprestimoResponseMapper.toDTONotificacao(emprestimo));
            }
        }

        return emprestimosNaoNotificados;
    }

    private boolean enviarNotificacaoAtraso(Emprestimo emprestimo) {
        LocalDate hoje = LocalDate.now();

        if (emprestimo.getDataUltimaNotificacao() != null && emprestimo.getDataUltimaNotificacao().isEqual(hoje)) {
            return false;
        }

        try {
            String assunto = "Empréstimo atrasado - Biblioteca";

            String dataPrazoFormatada = FormatarData.formatarData(emprestimo.getDataPrazo());
            String dataEmprestimoFormatada = FormatarData.formatarData(emprestimo.getDataEmprestimo());

            String mensagem = String.format(
                    """
                            Olá %s,
                            
                            Identificamos que o empréstimo do livro "%s" está atrasado.
                            A data de devolução era %s, e até o momento não registramos a devolução.
                            
                            Por favor, entregue o livro o mais breve possível ou renove o empréstimo para regularizar a situação.
                            
                            Detalhes do empréstimo:
                            - Livro: %s
                            - Data do empréstimo: %s
                            - Data para devolução: %s
                            - Situação atual: Atrasado
                            
                            Caso você já tenha devolvido o livro, por favor, entre em contato conosco para atualizarmos nossos registros.
                            
                            Atenciosamente,\s
                            Biblioteca Adelino Cunha.""",
                    emprestimo.getAluno().getNome(),
                    emprestimo.getExemplar().getLivro().getTitulo(),
                    dataPrazoFormatada,
                    emprestimo.getExemplar().getLivro().getTitulo(),
                    dataEmprestimoFormatada,
                    dataPrazoFormatada
            );
            boolean response = notificationService.sendNotification(emprestimo.getAluno().getEmail(), assunto, mensagem);
            if (!response){
                throw new RuntimeException(
                        "Falha ao enviar email para " + emprestimo.getAluno().getEmail()
                );
            }

            emprestimo.setDataUltimaNotificacao(hoje);
            emprestimoRepository.save(emprestimo);

            return true;

        } catch (Exception e) {
            return false;
        }
    }

    private boolean enviarNotificacaoPreAtraso(Emprestimo emprestimo) {
        LocalDate hoje = LocalDate.now();

        if (emprestimo.getDataUltimaNotificacao() != null && emprestimo.getDataUltimaNotificacao().isEqual(hoje)) {
            return false;
        }

        try {
            String assunto = "Lembrete: Empréstimo prestes a atrasar - Biblioteca";

            String dataPrazoFormatada = FormatarData.formatarData(emprestimo.getDataPrazo());
            String dataEmprestimoFormatada = FormatarData.formatarData(emprestimo.getDataEmprestimo());

            String mensagem = String.format(
                    "Olá %s,\n\n" +
                            "Este é apenas um lembrete de que o prazo de devolução do livro \"%s\" está se aproximando.\n" +
                            "A data de devolução é amanhã, %s.\n\n" +
                            "Por favor, entregue o livro até essa data ou renove o empréstimo para evitar atrasos.\n\n" +
                            "Detalhes do empréstimo:\n" +
                            "- Livro: %s\n" +
                            "- Data do empréstimo: %s\n" +
                            "- Data para devolução: %s\n" +
                            "- Situação atual: Pendente\n\n" +
                            "Caso você já tenha devolvido o livro, por favor, entre em contato conosco para atualizarmos nossos registros.\n\n" +
                            "Atenciosamente, \n" +
                            "Biblioteca Adelino Cunha.",
                    emprestimo.getAluno().getNome(),
                    emprestimo.getExemplar().getLivro().getTitulo(),
                    dataPrazoFormatada,
                    emprestimo.getExemplar().getLivro().getTitulo(),
                    dataEmprestimoFormatada,
                    dataPrazoFormatada
            );

            boolean response = notificationService.sendNotification(emprestimo.getAluno().getEmail(), assunto, mensagem);
            if (!response){
                throw new RuntimeException(
                        "Falha ao enviar email para " + emprestimo.getAluno().getEmail()
                );
            }

            emprestimo.setDataUltimaNotificacao(hoje);
            emprestimoRepository.save(emprestimo);

            return true;

        } catch (Exception e) {
            return false;
        }
    }

    private void verificarAtrasados(){
        LocalDate hoje = LocalDate.now();
        List<Emprestimo> emprestimosPendentes = emprestimoRepository.findBySituacao("pendente");

        for (Emprestimo emprestimo : emprestimosPendentes) {
            if (!emprestimo.getDataPrazo().isAfter(hoje)) {
                emprestimo.setSituacao("atrasado");
            }
        }

        emprestimoRepository.saveAll(emprestimosPendentes);
    }
}
