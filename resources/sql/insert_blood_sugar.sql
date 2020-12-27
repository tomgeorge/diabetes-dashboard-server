insert into blood_sugars(
  system_time,
  displayTime,
  value,
  realTimeValue,
  smoothedValue,
  status,
  trend,
  trendRate) values (
  :systemTime,
  :displayTime,
  :value,
  :realtimeValue,
  :smoothedValue,
  :status,
  :trend,
  :trendRate
)
