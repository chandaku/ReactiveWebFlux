/**
 * Digital Catalog
 * Partner Solutions and Technologies
 */
package com.example.demo.error;

import com.example.demo.exception.EntityNotFound;
import lombok.Getter;
import org.springframework.http.HttpStatus;
import reactor.core.publisher.Mono;

import javax.swing.text.html.parser.Entity;
import java.security.InvalidParameterException;

/**
 * @author Digital Catalog Dev Team
 *
 */
public class ExceptionTransform {

	@Getter
	private final HttpStatus httpStatus;

	@Getter
	private final String message;

	public ExceptionTransform(Throwable throwable) {
		this.httpStatus = getStatus(throwable);
		this.message = throwable.getMessage();
	}

	public HttpStatus getStatus(Throwable error) {
		if (error instanceof EntityNotFound) {
			return HttpStatus.NOT_FOUND;
		} else if (error instanceof InvalidParameterException) {
			return HttpStatus.BAD_REQUEST;
		} else {
			return HttpStatus.INTERNAL_SERVER_ERROR;
		}
	}

	public static <T extends Throwable> Mono<ExceptionTransform> translate(Mono<T> throwable) {
		System.out.print("In Translate");
		return throwable.flatMap(error ->
				Mono.just(
				new ExceptionTransform(error)
		));
	}
}
