package com.example.demo.error;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class ErrorHandler {
	
	public Mono<ServerResponse> throwableErrorOccurred(Throwable error) {
		log.error(error.getLocalizedMessage());
		return Mono.just(error).transform(this::getResponse);
	}

	<T extends Throwable> Mono<ServerResponse> getResponse(Mono<T> error) {
		return error.transform(ExceptionTransform::translate)
				.flatMap(trans -> ServerResponse.status(trans.getHttpStatus()).body(
						Mono.just(ErrorModel.builder().errorCode(trans.getHttpStatus().toString())
								.errorMessage(trans.getMessage()).build()),
						ErrorModel.class));
	}

}
