package fi.vm.yti.terminology.api.importapi.excel;

import java.util.UUID;

public class ExcelImportResponseDTO {
    UUID jobToken;
    String message;
    ErrorDetails errorDetails;

    public ExcelImportResponseDTO(UUID jobToken, String message) {
        this(jobToken, message, null);
    }

    public ExcelImportResponseDTO(UUID jobToken, String message, ErrorDetails errorDetails) {
        this.jobToken = jobToken;
        this.errorDetails = errorDetails;
        this.message = message;
    }

    public UUID getJobToken() {
        return jobToken;
    }

    public void setJobToken(UUID jobToken) {
        this.jobToken = jobToken;
    }

    public ErrorDetails getErrorDetails() {
        return errorDetails;
    }

    public void setErrorDetails(ErrorDetails errorDetails) {
        this.errorDetails = errorDetails;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public static class ErrorDetails {
        String sheet;
        Integer row;
        String column;

        public ErrorDetails(String sheet, Integer row, String column) {
            this.sheet = sheet;
            this.row = row;
            this.column = column;
        }

        public String getSheet() {
            return sheet;
        }

        public void setSheet(String sheet) {
            this.sheet = sheet;
        }

        public Integer getRow() {
            return row;
        }

        public void setRow(Integer row) {
            this.row = row;
        }

        public String getColumn() {
            return column;
        }

        public void setColumn(String column) {
            this.column = column;
        }
    }
}
