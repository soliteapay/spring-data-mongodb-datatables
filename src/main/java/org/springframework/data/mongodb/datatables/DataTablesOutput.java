package org.springframework.data.mongodb.datatables;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonView;
import lombok.Data;

import java.util.Collections;
import java.util.List;

@Data
@JsonInclude(value = Include.NON_NULL)
public final class DataTablesOutput<T> {

    /**
     * The draw counter that this object is a response to - from the draw parameter sent as part of
     * the data request. Note that it is strongly recommended for security reasons that you cast this
     * parameter to an integer, rather than simply echoing back to the client what it sent in the draw
     * parameter, in order to prevent Cross Site Scripting (XSS) attacks.
     */
    @JsonView(View.class)
    private int draw;

    /**
     * Total records, before filtering (i.e. the total number of records in the database)
     */
    @JsonView(View.class)
    private Long recordsTotal;

    /**
     * Total records, after filtering (i.e. the total number of records after filtering has been
     * applied - not just the number of records being returned for this page of data).
     */
    @JsonView(View.class)
    private Long recordsFiltered;

    /**
     * A boolean value expressing whether there are more records after the current selection (i.e. the current page is not the last one)
     */
    @JsonView(View.class)
    private boolean hasNext;

    public boolean getHasNext() {
        return hasNext;
    }

    /**
     * The data to be displayed in the table. This is an array of data source objects, one for each
     * row, which will be used by DataTables. Note that this parameter's name can be changed using the
     * ajaxDT option's dataSrc property.
     */
    @JsonView(View.class)
    private List<T> data = Collections.emptyList();

    /**
     * Optional: If an error occurs during the running of the server-side processing script, you can
     * inform the user of this error by passing back the error message to be displayed using this
     * parameter. Do not include if there is no error.
     */
    @JsonView(View.class)
    private String error;

    public interface View {
    }

}
