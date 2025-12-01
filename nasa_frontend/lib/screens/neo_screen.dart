import 'package:flutter/material.dart';
import 'package:fl_chart/fl_chart.dart';
import '../config/app_theme.dart';
import '../services/api_service.dart';
import '../config/api_config.dart';
import '../widgets/app_drawer.dart';

class NeoScreen extends StatefulWidget {
  final int selectedIndex;
  final Function(int) onNavigate;

  const NeoScreen({super.key, required this.selectedIndex, required this.onNavigate});

  @override
  State<NeoScreen> createState() => _NeoScreenState();
}

class _NeoScreenState extends State<NeoScreen> with SingleTickerProviderStateMixin{
  late TabController _tabController; // Dichiarazione del controller

  bool _isLoading = true;
  String _error = '';
  List<dynamic> _allAsteroids = [];
  List<dynamic> _hazardousAsteroids = [];
  Map<String, dynamic>? _stats;

  // Filters and sorting
  String _sortBy = 'name'; // name, size, magnitude
  String _sizeFilter = 'all'; // all, small, medium, large

  @override
  void initState() {
    super.initState();
    _tabController = TabController(length: 2, vsync: this); // 2 Tab: Lista e Statistiche
    _loadData();
  }

  @override
  void dispose() {
    _tabController.dispose(); // Rilascia le risorse del TabController
    super.dispose();
  }

  Future<void> _loadData() async {
    setState(() {
      _isLoading = true;
      _error = '';
    });

    try {
      final hazardous = await ApiService.getList('${ApiConfig.neoAsteroids}/hazardous');
      final all = await ApiService.getList(ApiConfig.neoAsteroids);
      final stats = await ApiService.getMap('${ApiConfig.neoAsteroids}/../stats');

      setState(() {
        _hazardousAsteroids = hazardous;
        _allAsteroids = all;
        _stats = stats;
        _isLoading = false;
      });
    } catch (e) {
      setState(() {
        _error = e.toString();
        _isLoading = false;
      });
    }
  }

  List<dynamic> _getFilteredAndSortedList() {
    var filtered = _hazardousAsteroids.where((asteroid) {
      if (_sizeFilter == 'all') return true;
      double? diameter = asteroid['estimatedDiameterKmMax'];
      if (diameter == null) return false;

      switch (_sizeFilter) {
        case 'small': return diameter < 0.5;
        case 'medium': return diameter >= 0.5 && diameter < 1.0;
        case 'large': return diameter >= 1.0;
        default: return true;
      }
    }).toList();

    switch (_sortBy) {
      case 'size':
        filtered.sort((a, b) {
          double aSize = a['estimatedDiameterKmMax'] ?? 0;
          double bSize = b['estimatedDiameterKmMax'] ?? 0;
          return bSize.compareTo(aSize);
        });
        break;
      case 'velocity':
        filtered.sort((a, b) {
          // Funzione helper per ottenere la velocità in modo sicuro
          double getVelocity(Map<String, dynamic> asteroid) {
            // 1. Accede a 'close_approach_data'. Se è null, restituisce null.
            final approachData = asteroid['close_approach_data'];
            // 2. Se la lista esiste e non è vuota, prendi il primo elemento [0].
            if (approachData is List && approachData.isNotEmpty) {
              final velocityStr = approachData[0]['relative_velocity']['kilometers_per_hour'];
              // 3. Tenta il parsing, se fallisce, usa 0.0.
              return double.tryParse(velocityStr.toString()) ?? 0.0;
            }
            return 0.0;
          }

          double aVelocity = getVelocity(a as Map<String, dynamic>);
          double bVelocity = getVelocity(b as Map<String, dynamic>);
          return bVelocity.compareTo(aVelocity); // Decrescente (più veloce prima)
        });
        break;
      case 'magnitude':
        filtered.sort((a, b) {
          double aMag = a['absoluteMagnitudeH'] ?? 99;
          double bMag = b['absoluteMagnitudeH'] ?? 99;
          return aMag.compareTo(bMag);
        });
        break;
      case 'name':
      default:
        filtered.sort((a, b) {
          String aName = a['name'] ?? '';
          String bName = b['name'] ?? '';
          return aName.compareTo(bName);
        });
    }

    return filtered;
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      drawer: AppDrawer(selectedIndex: widget.selectedIndex, onItemSelected: widget.onNavigate),
      body: Container(
        decoration: const BoxDecoration(
          gradient: LinearGradient(
            colors: [Color(0xFF6A1B9A), Color(0xFF8E24AA), Color(0xFFAB47BC)],
            begin: Alignment.topLeft,
            end: Alignment.bottomRight,
          ),
        ),
        child: SafeArea(
          child: Column(
            children: [
              _buildHeader(),
              _buildTabBar(context),
              Expanded(
                child: Container(
                  decoration: const BoxDecoration(
                    color: Colors.white,
                    borderRadius: BorderRadius.only(topLeft: Radius.circular(30), topRight: Radius.circular(30)),
                  ),
                  child: _isLoading
                      ? const Center(child: CircularProgressIndicator())
                      : _error.isNotEmpty
                      ? Center(child: Text('Error: $_error'))
                      : TabBarView(
                    controller: _tabController,
                    children: [
                      _buildAsteroidList(), // Tab 1: Lista "Normale"
                      _buildStatsContent(), // Tab 2: Grafici "Statistiche"
                    ],
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildTabBar(BuildContext context) {
    return Container(
      color: Colors.transparent, // Sfondo trasparente per mantenere il gradiente
      child: TabBar(
        controller: _tabController,
        indicatorSize: TabBarIndicatorSize.tab,
        indicator: BoxDecoration(
          color: Colors.white,
          borderRadius: BorderRadius.circular(16),
        ),
        tabs: const [
          Tab(text: 'Lista Asteroidi', icon: Icon(Icons.list)),
          Tab(text: 'Statistiche', icon: Icon(Icons.pie_chart)),
        ],
        labelStyle: const TextStyle(fontWeight: FontWeight.bold, fontSize: 14),
        labelColor: Colors.purple,
        unselectedLabelColor: Colors.white70,
        indicatorColor: Colors.white,
      ),
    );
  }

  Widget _buildHeader() {
    return Padding(
      padding: const EdgeInsets.all(24.0),
      child: Column(
        children: [
          Row(
            children: [
              Builder(
                builder: (context) => IconButton(
                  icon: const Icon(Icons.menu, color: Colors.white, size: 28),
                  onPressed: () => Scaffold.of(context).openDrawer(),
                ),
              ),
              Container(
                padding: const EdgeInsets.all(12),
                decoration: BoxDecoration(color: Colors.white.withOpacity(0.2), borderRadius: BorderRadius.circular(12)),
                child: const Icon(Icons.public, color: Colors.white, size: 32),
              ),
              const SizedBox(width: 16),
              const Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text('Near-Earth Objects', style: TextStyle(fontSize: 24, fontWeight: FontWeight.bold, color: Colors.white)),
                    Text('Potentially Hazardous Asteroids', style: TextStyle(fontSize: 14, color: Colors.white70)),
                  ],
                ),
              ),
            ],
          ),
          if (_stats != null) ...[
            const SizedBox(height: 16),
            Container(
              padding: const EdgeInsets.all(16),
              decoration: BoxDecoration(color: Colors.white.withOpacity(0.2), borderRadius: BorderRadius.circular(12)),
              child: Row(
                mainAxisAlignment: MainAxisAlignment.spaceAround,
                children: [
                  _buildHeaderStat('Total', '${_stats!['totalAsteroids']}'),
                  Container(width: 1, height: 30, color: Colors.white30),
                  _buildHeaderStat('Hazardous', '${_stats!['hazardousAsteroids']}'),
                  Container(width: 1, height: 30, color: Colors.white30),
                  _buildHeaderStat('Safe', '${_stats!['safeAsteroids']}'),
                ],
              ),
            ),
          ],
        ],
      ),
    );
  }

  Widget _buildHeaderStat(String label, String value) {
    return Column(
      children: [
        Text(value, style: const TextStyle(fontSize: 20, fontWeight: FontWeight.bold, color: Colors.white)),
        Text(label, style: const TextStyle(fontSize: 12, color: Colors.white70)),
      ],
    );
  }

  Widget _buildContent() {
    return SingleChildScrollView(
      padding: const EdgeInsets.all(24),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text('Statistics Overview', style: AppTheme.headlineMedium),
          const SizedBox(height: 16),
          _buildHazardPieChart(),
          const SizedBox(height: 32),
          const Text('Size Distribution', style: AppTheme.headlineMedium),
          const SizedBox(height: 16),
          _buildSizeDistribution(),
          const SizedBox(height: 32),
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              const Text('Potentially Hazardous', style: AppTheme.headlineMedium),
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                decoration: BoxDecoration(color: Colors.red, borderRadius: BorderRadius.circular(12)),
                child: Text('${_hazardousAsteroids.length} asteroids', style: const TextStyle(color: Colors.white, fontSize: 12, fontWeight: FontWeight.bold)),
              ),
            ],
          ),
          const SizedBox(height: 16),
          _buildFiltersBar(),
          const SizedBox(height: 16),
          if (_getFilteredAndSortedList().isEmpty)
            const Center(child: Padding(padding: EdgeInsets.all(32.0), child: Text('No asteroids match the filters')))
          else
            ...List.generate(_getFilteredAndSortedList().length, (index) {
              return _buildAsteroidCard(_getFilteredAndSortedList()[index]);
            }),
        ],
      ),
    );
  }

  Widget _buildFiltersBar() {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Colors.grey[100],
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: Colors.grey[300]!),
      ),
      child: Column(
        children: [
          Row(
            children: [
              const Icon(Icons.sort, size: 20, color: Colors.grey),
              const SizedBox(width: 8),
              const Text('Sort by:', style: TextStyle(fontWeight: FontWeight.w600)),
              const SizedBox(width: 12),
              Expanded(
                child: Wrap(
                  spacing: 8,
                  children: [
                    _buildSortChip('Name', 'name'),
                    _buildSortChip('Size', 'size'),
                    _buildSortChip('Velocity', 'velocity'),
                    _buildSortChip('Magnitude', 'magnitude'),
                  ],
                ),
              ),
            ],
          ),
          const SizedBox(height: 12),
          Row(
            children: [
              const Icon(Icons.filter_list, size: 20, color: Colors.grey),
              const SizedBox(width: 8),
              const Text('Size:', style: TextStyle(fontWeight: FontWeight.w600)),
              const SizedBox(width: 12),
              Expanded(
                child: Wrap(
                  spacing: 8,
                  children: [
                    _buildFilterChip('All', 'all'),
                    _buildFilterChip('Small', 'small'),
                    _buildFilterChip('Medium', 'medium'),
                    _buildFilterChip('Large', 'large'),
                  ],
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }

  // Nuovo Widget: Lista Filtrabile e Ordinabile
  Widget _buildAsteroidList() {
    final filteredAndSortedList = _getFilteredAndSortedList();

    return Column(
      children: [
        // La barra dei filtri e ordinamenti è sempre visibile
        _buildFiltersBar(),

        Expanded(
          child: filteredAndSortedList.isEmpty
              ? const Center(child: Padding(padding: EdgeInsets.all(32.0), child: Text('No asteroids match the filters')))
              : ListView.builder(
            padding: const EdgeInsets.all(24),
            itemCount: filteredAndSortedList.length,
            itemBuilder: (context, index) {
              return _buildAsteroidCard(filteredAndSortedList[index]);
            },
          ),
        ),
      ],
    );
  }

  // Nuovo Widget: Grafici e Riepiloghi
  Widget _buildStatsContent() {
    if (_stats == null) {
      return const Center(child: Text('Statistiche non disponibili.'));
    }

    return SingleChildScrollView(
      padding: const EdgeInsets.all(24),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // Statistiche Generali (Header già esistente)
          const Text('Rischio e Composizione Generale', style: AppTheme.headlineMedium),
          const SizedBox(height: 16),
          _buildHazardPieChart(), // Grafico a torta esistente
          const SizedBox(height: 32),

          // Distribuzione per Dimensione
          const Text('Distribuzione per Dimensione', style: AppTheme.headlineMedium),
          const SizedBox(height: 16),
          _buildSizeDistribution(), // Grafico a barre esistente
          const SizedBox(height: 32),

          // Tabella di Riepilogo (Opzionale: puoi aggiungere una tabella con i dati più estremi)
        ],
      ),
    );
  }

  Widget _buildSortChip(String label, String value) {
    bool isSelected = _sortBy == value;
    return ChoiceChip(
      label: Text(label),
      selected: isSelected,
      onSelected: (selected) {
        if (selected) setState(() => _sortBy = value);
      },
      selectedColor: Colors.purple,
      labelStyle: TextStyle(color: isSelected ? Colors.white : Colors.black87, fontWeight: isSelected ? FontWeight.bold : FontWeight.normal),
    );
  }

  Widget _buildFilterChip(String label, String value) {
    bool isSelected = _sizeFilter == value;
    return ChoiceChip(
      label: Text(label),
      selected: isSelected,
      onSelected: (selected) {
        if (selected) setState(() => _sizeFilter = value);
      },
      selectedColor: Colors.deepPurple,
      labelStyle: TextStyle(color: isSelected ? Colors.white : Colors.black87, fontWeight: isSelected ? FontWeight.bold : FontWeight.normal),
    );
  }

  Widget _buildHazardPieChart() {
    int hazardous = _stats?['hazardousAsteroids'] ?? 0;
    int safe = _stats?['safeAsteroids'] ?? 0;

    return Container(
      height: 300,
      padding: const EdgeInsets.all(16),
      decoration: AppTheme.cardDecoration,
      child: Row(
        children: [
          Expanded(
            child: PieChart(
              PieChartData(
                sections: [
                  PieChartSectionData(value: hazardous.toDouble(), title: '$hazardous\nHazardous', color: Colors.red, radius: 100, titleStyle: const TextStyle(fontSize: 16, fontWeight: FontWeight.bold, color: Colors.white)),
                  PieChartSectionData(value: safe.toDouble(), title: '$safe\nSafe', color: Colors.green, radius: 100, titleStyle: const TextStyle(fontSize: 16, fontWeight: FontWeight.bold, color: Colors.white)),
                ],
                sectionsSpace: 3,
                centerSpaceRadius: 50,
              ),
            ),
          ),
          const SizedBox(width: 24),
          Column(
            mainAxisAlignment: MainAxisAlignment.center,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              _buildLegendItem('Potentially Hazardous', Colors.red, hazardous),
              const SizedBox(height: 16),
              _buildLegendItem('Safe', Colors.green, safe),
              const SizedBox(height: 24),
              Text('${hazardous + safe > 0 ? ((hazardous / (hazardous + safe)) * 100).toStringAsFixed(1) : "0"}% Hazardous', style: const TextStyle(fontSize: 18, fontWeight: FontWeight.bold, color: Colors.red)),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildSizeDistribution() {
    Map<String, int> sizes = {'Small (< 0.5 km)': 0, 'Medium (0.5-1 km)': 0, 'Large (> 1 km)': 0};
    for (var asteroid in _allAsteroids) {
      double? diameter = asteroid['estimatedDiameterKmMax'];
      if (diameter == null) continue;
      if (diameter < 0.5) sizes['Small (< 0.5 km)'] = sizes['Small (< 0.5 km)']! + 1;
      else if (diameter < 1.0) sizes['Medium (0.5-1 km)'] = sizes['Medium (0.5-1 km)']! + 1;
      else sizes['Large (> 1 km)'] = sizes['Large (> 1 km)']! + 1;
    }
    int maxValue = sizes.values.isEmpty ? 1 : sizes.values.reduce((a, b) => a > b ? a : b);

    return Container(
      height: 250,
      padding: const EdgeInsets.all(16),
      decoration: AppTheme.cardDecoration,
      child: BarChart(
        BarChartData(
          alignment: BarChartAlignment.spaceAround,
          maxY: maxValue * 1.2,
          barGroups: [
            BarChartGroupData(x: 0, barRods: [BarChartRodData(toY: sizes['Small (< 0.5 km)']!.toDouble(), color: Colors.green, width: 60, borderRadius: BorderRadius.circular(8))]),
            BarChartGroupData(x: 1, barRods: [BarChartRodData(toY: sizes['Medium (0.5-1 km)']!.toDouble(), color: Colors.orange, width: 60, borderRadius: BorderRadius.circular(8))]),
            BarChartGroupData(x: 2, barRods: [BarChartRodData(toY: sizes['Large (> 1 km)']!.toDouble(), color: Colors.red, width: 60, borderRadius: BorderRadius.circular(8))]),
          ],
          titlesData: FlTitlesData(
            leftTitles: AxisTitles(sideTitles: SideTitles(showTitles: true, reservedSize: 40)),
            bottomTitles: AxisTitles(sideTitles: SideTitles(showTitles: true, getTitlesWidget: (value, meta) {
              List<String> labels = ['Small', 'Medium', 'Large'];
              if (value.toInt() >= 0 && value.toInt() < labels.length) {
                return Padding(padding: const EdgeInsets.only(top: 8), child: Text(labels[value.toInt()]));
              }
              return const Text('');
            })),
            topTitles: const AxisTitles(sideTitles: SideTitles(showTitles: false)),
            rightTitles: const AxisTitles(sideTitles: SideTitles(showTitles: false)),
          ),
          gridData: const FlGridData(show: false),
          borderData: FlBorderData(show: false),
        ),
      ),
    );
  }

  Widget _buildLegendItem(String label, Color color, int count) {
    return Row(
      children: [
        Container(width: 20, height: 20, decoration: BoxDecoration(color: color, shape: BoxShape.circle)),
        const SizedBox(width: 12),
        Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(label, style: const TextStyle(fontSize: 14, fontWeight: FontWeight.w500)),
            Text('$count asteroids', style: TextStyle(fontSize: 12, color: Colors.grey[600])),
          ],
        ),
      ],
    );
  }

  Widget _buildAsteroidCard(Map<String, dynamic> asteroid) {
    final name = asteroid['name'] ?? 'Unknown';
    final diameter = asteroid['estimatedDiameterKmMax'];
    final magnitude = asteroid['absoluteMagnitudeH'];
    final approachData = asteroid['close_approach_data'] != null && asteroid['close_approach_data'].isNotEmpty
        ? asteroid['close_approach_data'][0]
        : null;

    final fullDate = approachData?['close_approach_date_full'];
    final velocity = double.tryParse(approachData?['relative_velocity']?['kilometers_per_hour'] ?? '');
    final missDistanceLunar = double.tryParse(approachData?['miss_distance']?['lunar'] ?? '');


    return Container(
      margin: const EdgeInsets.only(bottom: 16),
      decoration: AppTheme.cardDecoration,
      child: Padding(
        padding: const EdgeInsets.all(20),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Container(
                  padding: const EdgeInsets.all(12),
                  decoration: BoxDecoration(color: Colors.red.withOpacity(0.1), borderRadius: BorderRadius.circular(12)),
                  child: const Icon(Icons.warning, color: Colors.red, size: 32),
                ),
                const SizedBox(width: 16),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(name, style: const TextStyle(fontSize: 16, fontWeight: FontWeight.bold), maxLines: 2, overflow: TextOverflow.ellipsis),
                      const SizedBox(height: 4),
                      Container(
                        padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                        decoration: BoxDecoration(color: Colors.red, borderRadius: BorderRadius.circular(8)),
                        child: const Text('POTENTIALLY HAZARDOUS', style: TextStyle(color: Colors.white, fontSize: 10, fontWeight: FontWeight.bold)),
                      ),
                    ],
                  ),
                ),
              ],
            ),
            const SizedBox(height: 16),
            const Divider(),
            const SizedBox(height: 12),
            if (fullDate != null)
              _buildInfoRow(Icons.access_time, 'Approach Date', fullDate),

            if (velocity != null)
              _buildInfoRow(Icons.rocket_launch, 'Rel. Velocity',
                  '${velocity.toStringAsFixed(0)} km/h',
                  isCritical: velocity > 80000), // Evidenzia se > 80,000 km/h

            if (missDistanceLunar != null)
              _buildInfoRow(Icons.place, 'Miss Distance',
                  '${missDistanceLunar.toStringAsFixed(1)} LD (Lunar)',
                  isCritical: missDistanceLunar < 50), // Evidenzia se passa molto vicino
            if (diameter != null) _buildInfoRow(Icons.straighten, 'Diameter', '${diameter.toStringAsFixed(2)} km'),
            if (magnitude != null) _buildInfoRow(Icons.brightness_5, 'Magnitude', magnitude.toStringAsFixed(1)),
          ],
        ),
      ),
    );
  }

  // _buildInfoRow con supporto colore
  Widget _buildInfoRow(IconData icon, String label, String value, {bool isCritical = false}) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 8),
      child: Row(
        children: [
          Icon(icon, size: 20, color: isCritical ? Colors.redAccent : Colors.grey[600]),
          const SizedBox(width: 8),
          Text('$label: ', style: TextStyle(fontSize: 14, color: Colors.grey[600])),
          Text(value, style: TextStyle(fontSize: 14, fontWeight: FontWeight.w500, color: isCritical ? Colors.red : Colors.black87)),
        ],
      ),
    );
  }
}