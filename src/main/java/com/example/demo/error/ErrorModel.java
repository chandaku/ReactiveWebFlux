/**
 * Digital Catalog
 * Partner Solutions and Technologies
 */
package com.example.demo.error;

import lombok.Builder;
import lombok.Data;

/**
 * @author Digital Catalog Dev Team
 *
 */
@Data
@Builder
public class ErrorModel {

	private String errorCode;
	private String errorMessage;
}
