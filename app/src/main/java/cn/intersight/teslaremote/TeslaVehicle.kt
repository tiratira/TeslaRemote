package cn.intersight.teslaremote

data class TeslaVehicle(
    var response: Response = Response()
)

data class VehicleInfo(
    var count: Int = 0,
    var response: List<Response> = listOf()
)

data class CommandResult(
    var reason: String = "",
    var result: Boolean = true
)

data class AuthResult(
    var access_token: String = "",
    var token_type: String = "",
    var expires_in: String = "",
    var refresh_token: String = "",
    var created_at: String = ""
)

data class Response(
    var api_version: Int = 0,
    var backseat_token: Any = Any(),
    var backseat_token_updated_at: Any = Any(),
    var calendar_enabled: String = "",
    var charge_state: ChargeState = ChargeState(),
    var climate_state: ClimateState = ClimateState(),
    var color: Any = Any(),
    var display_name: String = "",
    var drive_state: DriveState = DriveState(),
    var gui_settings: GuiSettings = GuiSettings(),
    var id: String = "",
    var id_s: String = "",
    var in_service: String = "",
    var option_codes: String = "",
    var state: String = "",
    var tokens: List<String> = listOf(),
    var user_id: Int = 0,
    var vehicle_config: VehicleConfig = VehicleConfig(),
    var vehicle_id: String = "",
    var vehicle_state: VehicleState = VehicleState(),
    var vin: String = ""
)

data class GuiSettings(
    var gui_24_hour_time: String = "",
    var gui_charge_rate_units: String = "",
    var gui_distance_units: String = "",
    var gui_range_display: String = "",
    var gui_temperature_units: String = "",
    var timestamp: Long = 0
)

data class ChargeState(
    var battery_heater_on: String = "",
    var battery_level: Int = 0,
    var battery_range: Double = 0.0,
    var charge_current_request: Int = 0,
    var charge_current_request_max: Int = 0,
    var charge_enable_request: String = "",
    var charge_energy_added: Double = 0.0,
    var charge_limit_soc: Int = 0,
    var charge_limit_soc_max: Int = 0,
    var charge_limit_soc_min: Int = 0,
    var charge_limit_soc_std: Int = 0,
    var charge_miles_added_ideal: Double = 0.0,
    var charge_miles_added_rated: Double = 0.0,
    var charge_port_door_open: String = "",
    var charge_port_latch: String = "",
    var charge_rate: Double = 0.0,
    var charge_to_max_range: String = "",
    var charger_actual_current: Int = 0,
    var charger_phases: Any = Any(),
    var charger_pilot_current: Int = 0,
    var charger_power: Int = 0,
    var charger_voltage: Int = 0,
    var charging_state: String = "",
    var conn_charge_cable: String = "",
    var est_battery_range: String = "",
    var fast_charger_brand: String = "",
    var fast_charger_present: String = "",
    var fast_charger_type: String = "",
    var ideal_battery_range: String = "",
    var managed_charging_active: String = "",
    var managed_charging_start_time: Any = Any(),
    var managed_charging_user_canceled: String = "",
    var max_range_charge_counter: Int = 0,
    var not_enough_power_to_heat: String = "",
    var scheduled_charging_pending: String = "",
    var scheduled_charging_start_time: Any = Any(),
    var time_to_full_charge: Double = 0.0,
    var timestamp: Long = 0,
    var trip_charging: String = "",
    var usable_battery_level: Int = 0,
    var user_charge_enable_request: Any = Any()
)

data class VehicleConfig(
    var can_accept_navigation_requests: String = "",
    var can_actuate_trunks: String = "",
    var car_special_type: String = "",
    var car_type: String = "",
    var charge_port_type: String = "",
    var eu_vehicle: String = "",
    var exterior_color: String = "",
    var has_air_suspension: String = "",
    var has_ludicrous_mode: String = "",
    var motorized_charge_port: String = "",
    var perf_config: String = "",
    var plg: String = "",
    var rear_seat_heaters: Int = 0,
    var rear_seat_type: Int = 0,
    var rhd: String = "",
    var roof_color: String = "",
    var seat_type: Int = 0,
    var spoiler_type: String = "",
    var sun_roof_installed: Int = 0,
    var third_row_seats: String = "",
    var timestamp: Long = 0,
    var trim_badging: String = "",
    var wheel_type: String = ""
)

data class VehicleState(
    var api_version: Int = 0,
    var autopark_state_v2: String = "",
    var autopark_style: String = "",
    var calendar_supported: String = "",
    var car_version: String = "",
    var center_display_state: Int = 0,
    var df: Int = 0,
    var dr: Int = 0,
    var ft: Int = 0,
    var homelink_nearby: String = "",
    var is_user_present: String = "",
    var last_autopark_error: String = "",
    var locked: String = "",
    var media_state: MediaState = MediaState(),
    var notifications_supported: String = "",
    var odometer: Double = 0.0,
    var parsed_calendar_supported: String = "",
    var pf: Int = 0,
    var pr: Int = 0,
    var remote_start: String = "",
    var remote_start_supported: String = "",
    var rt: Int = 0,
    var software_update: SoftwareUpdate = SoftwareUpdate(),
    var speed_limit_mode: SpeedLimitMode = SpeedLimitMode(),
    var sun_roof_percent_open: Int = 0,
    var sun_roof_state: String = "",
    var timestamp: Long = 0,
    var valet_mode: String = "",
    var valet_pin_needed: String = "",
    var vehicle_name: String = ""
)

data class MediaState(
    var remote_control_enabled: String = ""
)

data class SpeedLimitMode(
    var active: String = "",
    var current_limit_mph: Double = 0.0,
    var max_limit_mph: Int = 0,
    var min_limit_mph: Int = 0,
    var pin_code_set: String = ""
)

data class SoftwareUpdate(
    var expected_duration_sec: Int = 0,
    var status: String = ""
)

data class DriveState(
    var gps_as_of: Int = 0,
    var heading: Int = 0,
    var latitude: Double = 0.0,
    var longitude: Double = 0.0,
    var native_latitude: Double = 0.0,
    var native_location_supported: Int = 0,
    var native_longitude: Double = 0.0,
    var native_type: String = "",
    var power: Int = 0,
    var shift_state: Any = Any(),
    var speed: Any = Any(),
    var timestamp: Long = 0
)

data class ClimateState(
    var battery_heater: String = "",
    var battery_heater_no_power: String = "",
    var driver_temp_setting: Double = 0.0,
    var fan_status: Int = 0,
    var inside_temp: String = "",
    var is_auto_conditioning_on: Any = Any(),
    var is_climate_on: String = "",
    var is_front_defroster_on: String = "",
    var is_preconditioning: String = "",
    var is_rear_defroster_on: String = "",
    var left_temp_direction: Any = Any(),
    var max_avail_temp: Double = 0.0,
    var min_avail_temp: Double = 0.0,
    var outside_temp: Any = Any(),
    var passenger_temp_setting: Double = 0.0,
    var right_temp_direction: Any = Any(),
    var seat_heater_left: String = "",
    var seat_heater_rear_center: String = "",
    var seat_heater_rear_left: String = "",
    var seat_heater_rear_left_back: Int = 0,
    var seat_heater_rear_right: String = "",
    var seat_heater_rear_right_back: Int = 0,
    var seat_heater_right: String = "",
    var side_mirror_heaters: String = "",
    var smart_preconditioning: String = "",
    var steering_wheel_heater: String = "",
    var timestamp: Long = 0,
    var wiper_blade_heater: String = ""
)