package dev.barboza.tresemum.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import dev.barboza.tresemum.papeis.EstadoInvalidoException;
import dev.barboza.tresemum.papeis.RegraVioladaException;

/** Erros no formato problem+json (RFC 9457), com a mensagem pronta para mostrar na tela. */
@RestControllerAdvice
public class TratamentoDeErros {

    @ExceptionHandler(RegraVioladaException.class)
    ProblemDetail regra(RegraVioladaException e) {
        return problema(HttpStatus.UNPROCESSABLE_CONTENT, "Pedido recusado", e.getMessage());
    }

    @ExceptionHandler(EstadoInvalidoException.class)
    ProblemDetail estado(EstadoInvalidoException e) {
        return problema(HttpStatus.CONFLICT, "Não é possível agora", e.getMessage());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class})
    ProblemDetail entrada(Exception e) {
        return problema(HttpStatus.BAD_REQUEST, "Dados inválidos", "Confira os dados enviados.");
    }

    private static ProblemDetail problema(HttpStatus status, String titulo, String detalhe) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(status, detalhe);
        problema.setTitle(titulo);
        return problema;
    }
}
