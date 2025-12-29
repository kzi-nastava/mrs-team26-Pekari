package com.example.blackcar.presentation.auth.viewstate;

public abstract class LoginViewState {

    private LoginViewState() {}

    public static class Idle extends LoginViewState {
        @Override
        public String toString() {
            return "LoginViewState.Idle";
        }
    }

    public static class Loading extends LoginViewState {
        @Override
        public String toString() {
            return "LoginViewState.Loading";
        }
    }

    public static class Success extends LoginViewState {
        private final String userId;

        public Success(String userId) {
            this.userId = userId;
        }

        public String getUserId() {
            return userId;
        }

        @Override
        public String toString() {
            return "LoginViewState.Success(userId=" + userId + ")";
        }
    }

    public static class Error extends LoginViewState {
        private final String message;

        public Error(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }

        @Override
        public String toString() {
            return "LoginViewState.Error(message=" + message + ")";
        }
    }
}
