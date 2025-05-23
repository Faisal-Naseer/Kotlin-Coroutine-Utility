package datasource.weather
import datasource.weather.WeatherService
import kotlinx.coroutines.delay
import model.City
import model.Weather


object WeatherServiceImpl : WeatherService {
    override suspend fun fetchCities(): List<City> {
        delay(1000) // Simulate network delay
        return listOf(City("London"), City("Tokyo"), City("New York"))
    }

    override suspend fun fetchWeatherForCities(cities: List<City>): List<Weather> {
        delay(1000) // Simulate another API call
       // throw IllegalStateException("weather api failed")
        return cities.map {
            Weather(it.name, forecast = "Sunny 25°C")
        }
    }
}


