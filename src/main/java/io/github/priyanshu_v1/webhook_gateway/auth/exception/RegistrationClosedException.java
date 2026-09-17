package io.github.priyanshu_v1.webhook_gateway.auth.exception;

@SuppressWarnings("serial")
public class RegistrationClosedException extends RuntimeException {
	public RegistrationClosedException(String message) {
		super(message);
	}

}
