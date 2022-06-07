package fi.vm.yti.terminology.api.exception;

import org.apache.poi.ss.usermodel.Row;

public class ExcelParseException extends RuntimeException {

    private Integer rowNumber;
    private String sheet;
    private String column;
    private String reason;

    public ExcelParseException(String message) {
        super(message);
    }

    public ExcelParseException(String message, Row row, String column) {
        super(String.format("%s. Sheet: %s, Row: %d, Column: %s",
                message,
                row.getSheet().getSheetName(),
                row.getRowNum(),
                column)
        );

        this.rowNumber = row.getRowNum();
        this.column = column;
        this.sheet = row.getSheet().getSheetName();
        this.reason = message;
    }

    public ExcelParseException(String message, Row row, Integer columnIndex) {
        this(message, row, String.valueOf(columnIndex));
    }

    public ExcelParseException(String message, Row row) {
        super(String.format("%s. Sheet: %s, Row: %d",
                message,
                row.getSheet().getSheetName(),
                row.getRowNum())
        );

        this.reason = message;
        this.rowNumber = row.getRowNum();
        this.sheet = row.getSheet().getSheetName();
    }

    public Integer getRowNumber() {
        return rowNumber;
    }

    public String getSheet() {
        return sheet;
    }

    public String getColumn() {
        return column;
    }

    public String getReason() {
        return reason;
    }
}
