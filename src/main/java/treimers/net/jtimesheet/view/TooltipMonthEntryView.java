package treimers.net.jtimesheet.view;

import com.calendarfx.model.Entry;
import com.calendarfx.view.MonthEntryView;
import javafx.scene.control.Tooltip;

import java.util.function.Function;

/**
 * Month entry view that shows a tooltip on hover.
 * Use with {@link com.calendarfx.view.MonthView#setEntryViewFactory}.
 */
public class TooltipMonthEntryView extends MonthEntryView {

    public TooltipMonthEntryView(Entry<?> entry, Function<Entry<?>, String> tooltipTextProvider) {
        super(entry);
        String text = tooltipTextProvider != null ? tooltipTextProvider.apply(entry) : "";
        Tooltip.install(this, new Tooltip(text != null ? text : ""));
    }
}
