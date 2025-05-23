package datasource.weather

import model.City
import model.Weather

interface WeatherService {
    suspend fun fetchCities(): List<City>
    suspend fun fetchWeatherForCities(cities: List<City>): List<Weather>
}