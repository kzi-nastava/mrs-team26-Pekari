package com.example.blackcar.presentation.admin.viewstate;

public abstract class AddDriverViewState {

    private AddDriverViewState() {}

    public static class Idle extends AddDriverViewState {
        @Override
        public String toString() {
            return "AddDriverViewState.Idle";
        }
    }

    public static class Loading extends AddDriverViewState {
        @Override
        public String toString() {
            return "AddDriverViewState.Loading";
        }
    }

    public static class Success extends AddDriverViewState {
        private final String message;

        public Success(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }

        @Override
        public String toString() {
            return "AddDriverViewState.Success(message=" + message + ")";
        }
    }

    public static class Error extends AddDriverViewState {
        private final String message;

        public Error(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }

        @Override
        public String toString() {
            return "AddDriverViewState.Error(message=" + message + ")";
        }
    }
}
