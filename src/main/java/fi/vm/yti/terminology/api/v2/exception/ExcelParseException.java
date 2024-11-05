package fi.vm.yti.terminology.api.v2.exception;

import org.apache.poi.ss.usermodel.Row;

public class ExcelParseException extends RuntimeException {
    private final Integer rowNumber;
    private final String sheet;
    private final String column;
    private final String key;

    public ExcelParseException(String message) {
        super(message);
        this.key = message;
        this.rowNumber = null;
        this.sheet = null;
        this.column = null;
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
        this.key = message;
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

        this.key = message;
        this.rowNumber = row.getRowNum();
        this.sheet = row.getSheet().getSheetName();
        this.column = null;
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

    public String getKey() {
        return key;
    }
}
