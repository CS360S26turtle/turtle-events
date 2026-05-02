package com.example.peertutoringmarketplace;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Build;
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

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.text.SimpleDateFormat; // Use this for compatibility
import java.util.Calendar;       // Needed for the fallback formatter

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

    private static final int COLOR_SELECTED_BG   = 0xFF008080;
    private static final int COLOR_SELECTED_TEXT = 0xFFFFFFFF;
    private static final int COLOR_TODAY_TEXT    = 0xFF008080;
    private static final int COLOR_NORMAL_TEXT   = 0xFF00332B;
    private static final int COLOR_OTHER_MONTH   = 0xFFAAAAAA;

    // FIX: Use SimpleDateFormat so it doesn't crash on API < 26
    private final SimpleDateFormat monthFormatter =
            new SimpleDateFormat("MMMM yyyy", Locale.getDefault());

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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            selectedDate = LocalDate.now();
            YearMonth currentMonth = YearMonth.now();
            YearMonth startMonth = currentMonth.minusMonths(12);
            YearMonth endMonth = currentMonth.plusMonths(12);

            calendarView.setup(startMonth, endMonth, DayOfWeek.SUNDAY);
            calendarView.scrollToMonth(currentMonth);

            // Set initial month label
            Calendar cal = Calendar.getInstance();
            tvMonthYear.setText(monthFormatter.format(cal.getTime()));
        }

        calendarView.setDayBinder(new MonthDayBinder<DayViewContainer>() {
            @Override
            public DayViewContainer create(View view) {
                return new DayViewContainer(view);
            }

            @Override
            public void bind(DayViewContainer container, CalendarDay day) {
                container.calendarDay = day;
                LocalDate date = day.getDate();

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    container.textView.setText(String.valueOf(date.getDayOfMonth()));
                }

                container.textView.setBackground(null);
                container.eventDot.setVisibility(View.GONE);

                if (day.getPosition() != DayPosition.MonthDate) {
                    container.textView.setTextColor(COLOR_OTHER_MONTH);
                    container.getView().setOnClickListener(null);
                    return;
                }

                if (eventDates.contains(date)) {
                    container.eventDot.setVisibility(View.VISIBLE);
                }

                if (date.equals(selectedDate)) {
                    container.textView.setBackgroundResource(R.drawable.shape_selected_day);
                    container.textView.setTextColor(COLOR_SELECTED_TEXT);
                }
                else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    if (date.equals(LocalDate.now())) {
                        container.textView.setTextColor(COLOR_TODAY_TEXT);
                    } else {
                        container.textView.setTextColor(COLOR_NORMAL_TEXT);
                    }
                }

                container.getView().setOnClickListener(v -> {
                    LocalDate old = selectedDate;
                    selectedDate = date;
                    if (old != null) calendarView.notifyDateChanged(old);
                    calendarView.notifyDateChanged(date);
                    if (listener != null) listener.onDaySelected(date);
                });
            }
        });

        calendarView.setMonthScrollListener(calendarMonth -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Convert YearMonth to Date for the SimpleDateFormat
                Calendar cal = Calendar.getInstance();
                cal.set(calendarMonth.getYearMonth().getYear(), calendarMonth.getYearMonth().getMonthValue() - 1, 1);
                tvMonthYear.setText(monthFormatter.format(cal.getTime()));
            }
            return null;
        });

        btnPrev.setOnClickListener(v -> {
            CalendarMonth m = calendarView.findFirstVisibleMonth();
            if (m != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                calendarView.smoothScrollToMonth(m.getYearMonth().minusMonths(1));
            }
        });

        btnNext.setOnClickListener(v -> {
            CalendarMonth m = calendarView.findFirstVisibleMonth();
            if (m != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                calendarView.smoothScrollToMonth(m.getYearMonth().plusMonths(1));
            }
        });
    }

    public void setEventDates(Set<LocalDate> dates) {
        eventDates.clear();
        eventDates.addAll(dates);
        calendarView.notifyCalendarChanged();
    }

    public void selectDate(LocalDate date) {
        LocalDate old = selectedDate;
        selectedDate = date;
        if (old != null) calendarView.notifyDateChanged(old);
        calendarView.notifyDateChanged(date);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            calendarView.scrollToMonth(YearMonth.from(date));
        }
    }

    public LocalDate getSelectedDate() {
        return selectedDate;
    }
}