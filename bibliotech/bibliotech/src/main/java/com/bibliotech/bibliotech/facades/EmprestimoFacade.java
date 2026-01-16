package com.bibliotech.bibliotech.facades;

import com.bibliotech.bibliotech.dtos.request.EmprestimoRequestDTO;
import com.bibliotech.bibliotech.dtos.request.EmprestimoRequestDTOConcluir;
import com.bibliotech.bibliotech.dtos.request.mappers.EmprestimoRequestMapper;
import com.bibliotech.bibliotech.dtos.response.EmprestimoResponseDTO;
import com.bibliotech.bibliotech.dtos.response.mappers.EmprestimoResponseMapper;
import com.bibliotech.bibliotech.events.EmprestimoEvent;
import com.bibliotech.bibliotech.events.EventType;
import com.bibliotech.bibliotech.exception.NotFoundException;
import com.bibliotech.bibliotech.models.Aluno;
import com.bibliotech.bibliotech.models.Emprestimo;
import com.bibliotech.bibliotech.models.Exemplar;
import com.bibliotech.bibliotech.models.Usuario;
import com.bibliotech.bibliotech.observers.EmprestimoObserver;
import com.bibliotech.bibliotech.observers.EmprestimoSubject;
import com.bibliotech.bibliotech.repositories.EmprestimoRepository;
import com.bibliotech.bibliotech.repositories.UsuarioRepository;
import com.bibliotech.bibliotech.services.TokenService;
import com.bibliotech.bibliotech.validators.AlunoValidator;
import com.bibliotech.bibliotech.validators.EmprestimoValidator;
import com.bibliotech.bibliotech.validators.ExemplarValidator;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
public class EmprestimoFacade implements EmprestimoSubject {

    private static final Logger logger = LoggerFactory.getLogger(EmprestimoFacade.class);

    private final AlunoValidator alunoValidator;
    private final ExemplarValidator exemplarValidator;
    private final EmprestimoValidator emprestimoValidator;
    private final EmprestimoRepository emprestimoRepository;
    private final UsuarioRepository usuarioRepository;
    private final EmprestimoRequestMapper emprestimoRequestMapper;
    private final EmprestimoResponseMapper emprestimoResponseMapper;
    private final TokenService tokenService;

    private final List<EmprestimoObserver> observers = new ArrayList<>();

    public EmprestimoFacade(
            AlunoValidator alunoValidator,
            ExemplarValidator exemplarValidator,
            EmprestimoValidator emprestimoValidator,
            EmprestimoRepository emprestimoRepository,
            UsuarioRepository usuarioRepository,
            EmprestimoRequestMapper emprestimoRequestMapper,
            EmprestimoResponseMapper emprestimoResponseMapper,
            TokenService tokenService) {
        this.alunoValidator = alunoValidator;
        this.exemplarValidator = exemplarValidator;
        this.emprestimoValidator = emprestimoValidator;
        this.emprestimoRepository = emprestimoRepository;
        this.usuarioRepository = usuarioRepository;
        this.emprestimoRequestMapper = emprestimoRequestMapper;
        this.emprestimoResponseMapper = emprestimoResponseMapper;
        this.tokenService = tokenService;
    }


    @Override
    public void attach(EmprestimoObserver observer) {
        if (!observers.contains(observer)) {
            observers.add(observer);
            logger.debug("Observer {} anexado.", observer.getClass().getSimpleName());
        }
    }

    @Override
    public void detach(EmprestimoObserver observer) {
        observers.remove(observer);
    }

    @Override
    public void notifyObservers(EmprestimoEvent event) {
        logger.debug("Notificando {} observers sobre evento: {}", observers.size(), event.getType());
        for (EmprestimoObserver observer : observers) {
            try {
                observer.update(event);
            } catch (Exception e) {
                logger.error("Erro ao notificar observer {}: {}", observer.getClass().getSimpleName(), e.getMessage());
            }
        }
    }


    @Transactional
    public EmprestimoResponseDTO realizarEmprestimo(EmprestimoRequestDTO requestDTO) {
        Aluno aluno = alunoValidator.validarParaEmprestimo(requestDTO.getIdAluno());
        Exemplar exemplar = exemplarValidator.validarDisponibilidade(requestDTO.getIdExemplar());
        Usuario usuario = obterUsuarioAtual();

        Emprestimo emprestimo = criarEmprestimoEntidade(requestDTO, aluno, exemplar, usuario);
        
        alunoValidator.marcarComoDebito(aluno);
        exemplarValidator.marcarComoEmprestado(exemplar);

        Emprestimo emprestimoSalvo = emprestimoRepository.save(emprestimo);

        notifyObservers(new EmprestimoEvent(emprestimoSalvo, EventType.EMPRESTIMO_CRIADO));

        return emprestimoResponseMapper.toDto(emprestimoSalvo);
    }

    @Transactional
    public String cancelarEmprestimo(Integer id) {
        Emprestimo emprestimo = emprestimoValidator.validarExistencia(id);
        emprestimoValidator.validarParaCancelamento(emprestimo);

        Usuario usuario = obterUsuarioAtual();

        emprestimo.setSituacao("cancelado");
        emprestimo.setConcluidoPor(usuario);
        emprestimo.setDataConclusao(LocalDate.now());

        alunoValidator.marcarComoRegular(emprestimo.getAluno());
        exemplarValidator.marcarComoDisponivel(emprestimo.getExemplar());

        Emprestimo emprestimoSalvo = emprestimoRepository.save(emprestimo);
        
        notifyObservers(new EmprestimoEvent(emprestimoSalvo, EventType.EMPRESTIMO_CANCELADO));

        return "Emprestimo cancelado com sucesso.";
    }

    @Transactional
    public String concluirEmprestimo(Integer id, EmprestimoRequestDTOConcluir dtoConcluir) {
        Emprestimo emprestimo = emprestimoValidator.validarExistencia(id);
        emprestimoValidator.validarParaConclusao(emprestimo);

        Usuario usuario = obterUsuarioAtual();

        emprestimo.setObservacao(dtoConcluir.getObservacao());
        emprestimo.setDataConclusao(LocalDate.now());
        emprestimo.setConcluidoPor(usuario);

        EventType tipoEvento;

        if (dtoConcluir.isExtraviado()) {
            emprestimo.setSituacao("extraviado");
            alunoValidator.marcarComoIrregular(emprestimo.getAluno());
            exemplarValidator.marcarComoExtraviado(emprestimo.getExemplar());
            tipoEvento = EventType.EMPRESTIMO_EXTRAVIADO;
        } else {
            emprestimo.setSituacao("entregue");
            alunoValidator.marcarComoRegular(emprestimo.getAluno());
            exemplarValidator.marcarComoDisponivel(emprestimo.getExemplar());
            tipoEvento = EventType.EMPRESTIMO_CONCLUIDO;
        }

        Emprestimo emprestimoSalvo = emprestimoRepository.save(emprestimo);
        
        notifyObservers(new EmprestimoEvent(emprestimoSalvo, tipoEvento, dtoConcluir.getObservacao()));

        return dtoConcluir.isExtraviado() ? "Emprestimo extraviado com sucesso." : "Emprestimo concluido com sucesso.";
    }

    @Transactional
    public String renovarPrazo(Integer id) {
        Emprestimo emprestimo = emprestimoValidator.validarExistencia(id);
        emprestimoValidator.validarParaRenovacao(emprestimo);

        if ("atrasado".equals(emprestimo.getSituacao())) {
            emprestimo.setDataPrazo(LocalDate.now().plusDays(7));
            emprestimo.setSituacao("pendente");
        } else {
            emprestimo.setDataPrazo(emprestimo.getDataPrazo().plusDays(7));
        }
        emprestimo.setQtdRenovacao(emprestimo.getQtdRenovacao() + 1);

        Emprestimo emprestimoSalvo = emprestimoRepository.save(emprestimo);
        
        notifyObservers(new EmprestimoEvent(emprestimoSalvo, EventType.EMPRESTIMO_RENOVADO));

        return "Prazo renovado com sucesso.";
    }


    private Usuario obterUsuarioAtual() {
        return usuarioRepository.findById(tokenService.getUsuarioId())
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado"));
    }

    private Emprestimo criarEmprestimoEntidade(EmprestimoRequestDTO dto, Aluno aluno, Exemplar exemplar, Usuario usuario) {
        Emprestimo emprestimo = emprestimoRequestMapper.toEntity(dto);
        emprestimo.setAluno(aluno);
        emprestimo.setExemplar(exemplar);
        emprestimo.setRealizadoPor(usuario);
        emprestimo.setSituacao("pendente");
        return emprestimo;
    }
}