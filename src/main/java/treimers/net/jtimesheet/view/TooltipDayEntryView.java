package treimers.net.jtimesheet.view;

import com.calendarfx.model.Entry;
import com.calendarfx.view.DayEntryView;
import javafx.scene.control.Tooltip;

import java.util.function.Function;

/**
 * Day entry view that shows a tooltip on hover.
 * Use with {@link com.calendarfx.view.DayView#setEntryViewFactory}.
 */
public class TooltipDayEntryView extends DayEntryView {

    public TooltipDayEntryView(Entry<?> entry, Function<Entry<?>, String> tooltipTextProvider) {
        super(entry);
        String text = tooltipTextProvider != null ? tooltipTextProvider.apply(entry) : "";
        Tooltip.install(this, new Tooltip(text != null ? text : ""));
    }
}
