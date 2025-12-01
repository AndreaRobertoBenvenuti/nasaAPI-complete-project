import 'package:flutter/material.dart';
import 'screens/home_screen.dart';
import 'screens/analysis_screen.dart';
import 'screens/solar_activity_screen.dart'; // Nuovo screen unificato
import 'screens/fireballs_screen.dart';
import 'screens/neo_screen.dart';
import 'config/app_theme.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'NASA Dashboard',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(seedColor: AppTheme.primaryBlue),
        useMaterial3: true,
        fontFamily: 'Roboto',
      ),
      home: const MainNavigator(),
    );
  }
}

class MainNavigator extends StatefulWidget {
  const MainNavigator({super.key});

  @override
  State<MainNavigator> createState() => _MainNavigatorState();
}

class _MainNavigatorState extends State<MainNavigator> {
  int _selectedIndex = 0;

  void _onNavigate(int index) {
    setState(() {
      _selectedIndex = index;
    });
  }

  @override
  Widget build(BuildContext context) {
    Widget currentScreen;

    switch (_selectedIndex) {
      case 0:
        currentScreen = HomeScreen(selectedIndex: _selectedIndex, onNavigate: _onNavigate);
        break;
      case 1:
        currentScreen = AnalysisScreen(selectedIndex: _selectedIndex, onNavigate: _onNavigate);
        break;
      case 2:
      // Questo sostituisce sia SolarEvents che IPS
        currentScreen = SolarActivityScreen(selectedIndex: _selectedIndex, onNavigate: _onNavigate);
        break;
      case 3:
        currentScreen = FireballsScreen(selectedIndex: _selectedIndex, onNavigate: _onNavigate);
        break;
      case 4:
        currentScreen = NeoScreen(selectedIndex: _selectedIndex, onNavigate: _onNavigate);
        break;
      default:
        currentScreen = HomeScreen(selectedIndex: _selectedIndex, onNavigate: _onNavigate);
    }

    return currentScreen;
  }
}