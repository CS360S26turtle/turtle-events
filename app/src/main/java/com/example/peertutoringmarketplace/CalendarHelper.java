package com.example.peertutoringmarketplace;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.fragment.app.FragmentActivity;

import com.kizitonwose.calendar.core.CalendarDay;
import com.kizitonwose.calendar.core.CalendarMonth;
import com.kizitonwose.calendar.core.DayPosition;
import com.kizitonwose.calendar.view.CalendarView;
import com.kizitonwose.calendar.view.MonthDayBinder;
import com.kizitonwose.calendar.view.ViewContainer;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Shared helper that wires up a Kizitonwose CalendarView with
 * teal selection circles, today indicator, event dots, and
 * prev/next month navigation.
 */
public class CalendarHelper {

    public interface OnDaySelectedListener {
        void onDaySelected(LocalDate date);
    }

    public static class DayViewContainer extends ViewContainer {
        public final TextView textView;
        public final View eventDot;
        public CalendarDay calendarDay;

        public DayViewContainer(View view) {
            super(view);
            textView = view.findViewById(R.id.calendarDayText);
            eventDot = view.findViewById(R.id.eventDot);
        }
    }

    private final CalendarView calendarView;
    private final TextView tvMonthYear;
    private final ImageButton btnPrev;
    private final ImageButton btnNext;
    private final FragmentActivity activity;

    private LocalDate selectedDate;
    private final Set<LocalDate> eventDates = new HashSet<>();
    private OnDaySelectedListener listener;

    private static final int COLOR_SELECTED_BG   = 0xFF008080; // teal
    private static final int COLOR_SELECTED_TEXT = 0xFFFFFFFF; // white
    private static final int COLOR_TODAY_TEXT    = 0xFF008080; // teal outline
    private static final int COLOR_NORMAL_TEXT   = 0xFF00332B; // dark green
    private static final int COLOR_OTHER_MONTH   = 0xFFAAAAAA; // grey

    private final DateTimeFormatter monthFormatter =
            DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault());

    public CalendarHelper(FragmentActivity activity,
                          CalendarView calendarView,
                          View headerView,
                          OnDaySelectedListener listener) {
        this.activity     = activity;
        this.calendarView = calendarView;
        this.listener     = listener;

        tvMonthYear = headerView.findViewById(R.id.tvMonthYear);
        btnPrev     = headerView.findViewById(R.id.btnPrevMonth);
        btnNext     = headerView.findViewById(R.id.btnNextMonth);
    }

    public void setup() {
        selectedDate = LocalDate.now();
        YearMonth currentMonth = YearMonth.now();
        YearMonth startMonth   = currentMonth.minusMonths(12);
        YearMonth endMonth     = currentMonth.plusMonths(12);

        calendarView.setup(startMonth, endMonth, java.time.DayOfWeek.SUNDAY);
        calendarView.scrollToMonth(currentMonth);

        calendarView.setDayBinder(new MonthDayBinder<DayViewContainer>() {
            @Override
            public DayViewContainer create(View view) {
                return new DayViewContainer(view);
            }

            @Override
            public void bind(DayViewContainer container, CalendarDay day) {
                container.calendarDay = day;
                LocalDate date = day.getDate();
                container.textView.setText(String.valueOf(date.getDayOfMonth()));

                // Reset
                container.textView.setBackground(null);
                container.eventDot.setVisibility(View.GONE);

                if (day.getPosition() != DayPosition.MonthDate) {
                    // Out-of-month days
                    container.textView.setTextColor(COLOR_OTHER_MONTH);
                    container.getView().setOnClickListener(null);
                    return;
                }

                // Event dot
                if (eventDates.contains(date)) {
                    container.eventDot.setVisibility(View.VISIBLE);
                }

                // Selected day: teal filled circle, white text
                if (date.equals(selectedDate)) {
                    container.textView.setBackgroundResource(R.drawable.shape_selected_day);
                    container.textView.setTextColor(COLOR_SELECTED_TEXT);
                }
                // Today (not selected): teal text only
                else if (date.equals(LocalDate.now())) {
                    container.textView.setTextColor(COLOR_TODAY_TEXT);
                }
                // Normal day
                else {
                    container.textView.setTextColor(COLOR_NORMAL_TEXT);
                }

                container.getView().setOnClickListener(v -> {
                    LocalDate old = selectedDate;
                    selectedDate = date;

                    // Refresh both old and new cells
                    if (old != null) calendarView.notifyDateChanged(old);
                    calendarView.notifyDateChanged(date);

                    if (listener != null) listener.onDaySelected(date);
                });
            }
        });

        calendarView.setMonthScrollListener(calendarMonth -> {
            tvMonthYear.setText(calendarMonth.getYearMonth().format(monthFormatter));
            return null;
        });

        btnPrev.setOnClickListener(v -> {
            calendarView.findFirstVisibleMonth();
            CalendarMonth m = calendarView.findFirstVisibleMonth();
            if (m != null) calendarView.smoothScrollToMonth(m.getYearMonth().minusMonths(1));
        });

        btnNext.setOnClickListener(v -> {
            CalendarMonth m = calendarView.findFirstVisibleMonth();
            if (m != null) calendarView.smoothScrollToMonth(m.getYearMonth().plusMonths(1));
        });

        // Show month label immediately
        tvMonthYear.setText(currentMonth.format(monthFormatter));
    }

    /** Mark dates with event dots (pass dates that have slots). */
    public void setEventDates(Set<LocalDate> dates) {
        eventDates.clear();
        eventDates.addAll(dates);
        calendarView.notifyCalendarChanged();
    }

    /** Programmatically select a date and notify listener. */
    public void selectDate(LocalDate date) {
        LocalDate old = selectedDate;
        selectedDate = date;
        if (old != null) calendarView.notifyDateChanged(old);
        calendarView.notifyDateChanged(date);
        calendarView.scrollToMonth(YearMonth.from(date));
    }

    public LocalDate getSelectedDate() {
        return selectedDate;
    }
}
