package campus.reg.model;

import java.util.List;

/** One page of rows plus the numbers needed for "Page x of y" and the Prev/Next buttons. */
public record PageData(List<RegistrationRow> rows, int totalRows, int pageNo, int pageCount) {
    public boolean hasPrev() { return pageNo > 1; }
    public boolean hasNext() { return pageNo < pageCount; }
}
