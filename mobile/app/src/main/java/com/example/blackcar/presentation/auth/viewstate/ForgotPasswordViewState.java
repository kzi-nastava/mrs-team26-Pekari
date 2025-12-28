package com.example.blackcar.presentation.auth.viewstate;

public abstract class ForgotPasswordViewState {

    private ForgotPasswordViewState() {}

    public static class Idle extends ForgotPasswordViewState {
        @Override
        public String toString() {
            return "ForgotPasswordViewState.Idle";
        }
    }

    public static class Loading extends ForgotPasswordViewState {
        @Override
        public String toString() {
            return "ForgotPasswordViewState.Loading";
        }
    }

    public static class Success extends ForgotPasswordViewState {
        @Override
        public String toString() {
            return "ForgotPasswordViewState.Success";
        }
    }

    public static class Error extends ForgotPasswordViewState {
        private final String message;

        public Error(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }

        @Override
        public String toString() {
            return "ForgotPasswordViewState.Error(message=" + message + ")";
        }
    }
}
