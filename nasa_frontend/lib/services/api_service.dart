import 'dart:convert';
import 'package:http/http.dart' as http;
import '../config/api_config.dart';

class ApiService {
  static Future<dynamic> get(String endpoint) async {
    try {
      final response = await http
          .get(
        Uri.parse(endpoint),
        headers: {'Content-Type': 'application/json'},
      )
          .timeout(ApiConfig.timeout);

      if (response.statusCode == 200) {
        return json.decode(response.body);
      } else {
        throw Exception('Failed to load data: ${response.statusCode}');
      }
    } catch (e) {
      throw Exception('Network error: $e');
    }
  }

  static Future<List<dynamic>> getList(String endpoint) async {
    final data = await get(endpoint);
    return data as List<dynamic>;
  }

  static Future<Map<String, dynamic>> getMap(String endpoint) async {
    final data = await get(endpoint);
    return data as Map<String, dynamic>;
  }
}