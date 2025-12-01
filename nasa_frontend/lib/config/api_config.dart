class ApiConfig {
  // IMPORTANTE: Cambia questo con l'IP del tuo computer se testi su device fisico
  // Per emulatore Android: usa 10.0.2.2
  // Per emulatore iOS: usa localhost
  // Per device fisico: usa l'IP del tuo computer (es: 192.168.1.100)


  static const String baseUrl = 'http://localhost:8080/api';

  // Endpoints
  static const String fireballs = '$baseUrl/fireballs';
  static const String solarFlares = '$baseUrl/solar-flares';
  static const String cme = '$baseUrl/solar-events/cme';
  static const String storms = '$baseUrl/solar-events/storms';
  static const String ips = '$baseUrl/ips';
  static const String neoAsteroids = '$baseUrl/neo/asteroids';
  static const String neoApproaches = '$baseUrl/neo/approaches';
  static const String analysis = '$baseUrl/analysis';

  // Timeouts
  static const Duration timeout = Duration(seconds: 30);
}