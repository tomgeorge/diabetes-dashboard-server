insert into blood_sugars(
  system_time,
  display_time,
  value,
  realtime_value,
  smoothed_value,
  status,
  trend,
  trend_rate) values (
  :systemTime,
  :displayTime,
  :value,
  :realtimeValue,
  :smoothedValue,
  :status,
  :trend,
  :trendRate
)
