package com.example.blackcar.presentation.auth.viewstate;

public abstract class RegisterViewState {

    private RegisterViewState() {}

    public static class Idle extends RegisterViewState {
        @Override
        public String toString() {
            return "RegisterViewState.Idle";
        }
    }

    public static class Loading extends RegisterViewState {
        @Override
        public String toString() {
            return "RegisterViewState.Loading";
        }
    }

    public static class Success extends RegisterViewState {
        @Override
        public String toString() {
            return "RegisterViewState.Success";
        }
    }

    public static class Error extends RegisterViewState {
        private final String message;

        public Error(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }

        @Override
        public String toString() {
            return "RegisterViewState.Error(message=" + message + ")";
        }
    }
}
