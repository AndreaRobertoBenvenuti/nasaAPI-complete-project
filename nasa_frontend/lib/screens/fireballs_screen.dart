import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'package:fl_chart/fl_chart.dart';
import '../config/app_theme.dart';
import '../services/fireball_service.dart';
import '../models/fireball.dart';
import '../widgets/app_drawer.dart';

class FireballsScreen extends StatefulWidget {
  final int selectedIndex;
  final Function(int) onNavigate;

  const FireballsScreen({
    super.key,
    required this.selectedIndex,
    required this.onNavigate,
  });

  @override
  State<FireballsScreen> createState() => _FireballsScreenState();
}

class _FireballsScreenState extends State<FireballsScreen> {
  bool _isLoading = true;
  String _error = '';
  List<Fireball> _fireballs = [];
  List<Fireball> _topEnergy = [];
  Map<int, int> _monthlyDistribution = {};
  Map<String, int> _energyDistribution = {};

  // Filters and sorting
  String _sortBy = 'date'; // date, energy, velocity
  double _minEnergy = 0.0;

  @override
  void initState() {
    super.initState();
    _loadData();
  }

  Future<void> _loadData() async {
    setState(() {
      _isLoading = true;
      _error = '';
    });

    try {
      final all = await FireballService.getAll();
      final top = await FireballService.getTopEnergy(limit: 20);

      // Calcola distribuzione mensile
      Map<int, int> monthly = {};
      for (var f in all) {
        int month = f.eventDate.month;
        monthly[month] = (monthly[month] ?? 0) + 1;
      }

      // Calcola distribuzione energia
      Map<String, int> energy = {
        'Low (< 0.1 kt)': 0,
        'Medium (0.1-1 kt)': 0,
        'High (1-10 kt)': 0,
        'Very High (> 10 kt)': 0,
      };

      for (var f in all) {
        if (f.totalImpactEnergyKt == null) continue;
        double e = f.totalImpactEnergyKt!;
        if (e < 0.1) energy['Low (< 0.1 kt)'] = energy['Low (< 0.1 kt)']! + 1;
        else if (e < 1) energy['Medium (0.1-1 kt)'] = energy['Medium (0.1-1 kt)']! + 1;
        else if (e < 10) energy['High (1-10 kt)'] = energy['High (1-10 kt)']! + 1;
        else energy['Very High (> 10 kt)'] = energy['Very High (> 10 kt)']! + 1;
      }

      setState(() {
        _fireballs = all;
        _topEnergy = top;
        _monthlyDistribution = monthly;
        _energyDistribution = energy;
        _isLoading = false;
      });
    } catch (e) {
      setState(() {
        _error = e.toString();
        _isLoading = false;
      });
    }
  }

  List<Fireball> _getFilteredAndSortedList() {
    var filtered = _fireballs.where((f) {
      if (f.totalImpactEnergyKt == null) return _minEnergy == 0;
      return f.totalImpactEnergyKt! >= _minEnergy;
    }).toList();

    switch (_sortBy) {
      case 'energy':
        filtered.sort((a, b) {
          double aEnergy = a.totalImpactEnergyKt ?? 0;
          double bEnergy = b.totalImpactEnergyKt ?? 0;
          return bEnergy.compareTo(aEnergy);
        });
        break;
      case 'velocity':
        filtered.sort((a, b) {
          double aVel = a.velocityKmS ?? 0;
          double bVel = b.velocityKmS ?? 0;
          return bVel.compareTo(aVel);
        });
        break;
      case 'date':
      default:
        filtered.sort((a, b) => b.eventDate.compareTo(a.eventDate));
    }

    return filtered;
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      drawer: AppDrawer(
        selectedIndex: widget.selectedIndex,
        onItemSelected: widget.onNavigate,
      ),
      body: Container(
        decoration: const BoxDecoration(
          gradient: LinearGradient(
            colors: [Color(0xFFFF6F00), Color(0xFFFF8F00)],
            begin: Alignment.topLeft,
            end: Alignment.bottomRight,
          ),
        ),
        child: SafeArea(
          child: Column(
            children: [
              _buildHeader(),
              Expanded(
                child: Container(
                  decoration: const BoxDecoration(
                    color: Colors.white,
                    borderRadius: BorderRadius.only(
                      topLeft: Radius.circular(30),
                      topRight: Radius.circular(30),
                    ),
                  ),
                  child: _isLoading
                      ? const Center(child: CircularProgressIndicator())
                      : _error.isNotEmpty
                      ? Center(child: Text('Error: $_error'))
                      : _buildContent(),
                ),
              ),
            ],
          ),
        ),
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
                  onPressed: () {
                    Scaffold.of(context).openDrawer();
                  },
                ),
              ),
              Container(
                padding: const EdgeInsets.all(12),
                decoration: BoxDecoration(
                  color: Colors.white.withOpacity(0.2),
                  borderRadius: BorderRadius.circular(12),
                ),
                child: const Icon(Icons.flash_on, color: Colors.white, size: 32),
              ),
              const SizedBox(width: 16),
              const Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      'Fireball Events',
                      style: TextStyle(
                        fontSize: 24,
                        fontWeight: FontWeight.bold,
                        color: Colors.white,
                      ),
                    ),
                    Text(
                      'Meteorite atmospheric entries',
                      style: TextStyle(
                        fontSize: 14,
                        color: Colors.white70,
                      ),
                    ),
                  ],
                ),
              ),
            ],
          ),
          const SizedBox(height: 16),
          Container(
            padding: const EdgeInsets.all(16),
            decoration: BoxDecoration(
              color: Colors.white.withOpacity(0.2),
              borderRadius: BorderRadius.circular(12),
            ),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.spaceAround,
              children: [
                _buildHeaderStat('Total', '${_fireballs.length}'),
                Container(width: 1, height: 30, color: Colors.white30),
                _buildHeaderStat('With Location', '${_fireballs.where((f) => f.hasLocation).length}'),
                Container(width: 1, height: 30, color: Colors.white30),
                _buildHeaderStat('Top Energy', '${_topEnergy.length}'),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildHeaderStat(String label, String value) {
    return Column(
      children: [
        Text(
          value,
          style: const TextStyle(
            fontSize: 20,
            fontWeight: FontWeight.bold,
            color: Colors.white,
          ),
        ),
        Text(
          label,
          style: const TextStyle(
            fontSize: 12,
            color: Colors.white70,
          ),
        ),
      ],
    );
  }

  Widget _buildContent() {
    return DefaultTabController(
      length: 2,
      child: Column(
        children: [
          Container(
            margin: const EdgeInsets.all(24),
            decoration: BoxDecoration(
              color: Colors.grey[200],
              borderRadius: BorderRadius.circular(12),
            ),
            child: TabBar(
              indicator: BoxDecoration(
                color: Colors.orange,
                borderRadius: BorderRadius.circular(12),
              ),
              labelColor: Colors.white,
              unselectedLabelColor: Colors.black54,
              tabs: const [
                Tab(text: 'All Events'),
                Tab(text: 'Statistics'),
              ],
            ),
          ),
          Expanded(
            child: TabBarView(
              children: [
                _buildAllEventsList(),
                _buildStatisticsTab(),
              ],
            ),
          ),
        ],
      ),
    );
  }


  Widget _buildAllEventsList() {
    final filteredList = _getFilteredAndSortedList();

    return Column(
      children: [
        _buildFiltersBar(),
        Expanded(
          child: filteredList.isEmpty
              ? const Center(child: Text('No events match the filters'))
              : ListView.builder(
            padding: const EdgeInsets.symmetric(horizontal: 24),
            itemCount: filteredList.length,
            itemBuilder: (context, index) {
              return _buildFireballCard(filteredList[index], null);
            },
          ),
        ),
      ],
    );
  }

  Widget _buildFiltersBar() {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Colors.grey[100],
        border: Border(
          bottom: BorderSide(color: Colors.grey[300]!),
        ),
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
                    _buildSortChip('Date', 'date'),
                    _buildSortChip('Energy', 'energy'),
                    _buildSortChip('Velocity', 'velocity'),
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
              const Text('Min Energy:', style: TextStyle(fontWeight: FontWeight.w600)),
              const SizedBox(width: 12),
              Expanded(
                child: Wrap(
                  spacing: 8,
                  children: [
                    _buildFilterChip('All', 0.0),
                    _buildFilterChip('> 0.1 kt', 0.1),
                    _buildFilterChip('> 1 kt', 1.0),
                    _buildFilterChip('> 5 kt', 5.0),
                  ],
                ),
              ),
            ],
          ),
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
        if (selected) {
          setState(() {
            _sortBy = value;
          });
        }
      },
      selectedColor: Colors.orange,
      labelStyle: TextStyle(
        color: isSelected ? Colors.white : Colors.black87,
        fontWeight: isSelected ? FontWeight.bold : FontWeight.normal,
      ),
    );
  }

  Widget _buildFilterChip(String label, double value) {
    bool isSelected = _minEnergy == value;
    return ChoiceChip(
      label: Text(label),
      selected: isSelected,
      onSelected: (selected) {
        if (selected) {
          setState(() {
            _minEnergy = value;
          });
        }
      },
      selectedColor: Colors.deepOrange,
      labelStyle: TextStyle(
        color: isSelected ? Colors.white : Colors.black87,
        fontWeight: isSelected ? FontWeight.bold : FontWeight.normal,
      ),
    );
  }

  Widget _buildStatisticsTab() {
    return SingleChildScrollView(
      padding: const EdgeInsets.all(24),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text('Monthly Distribution', style: AppTheme.headlineMedium),
          const SizedBox(height: 16),
          _buildMonthlyChart(),
          const SizedBox(height: 32),
          const Text('Energy Distribution', style: AppTheme.headlineMedium),
          const SizedBox(height: 16),
          _buildEnergyChart(),
          const SizedBox(height: 32),
          _buildSummaryCards(),
        ],
      ),
    );
  }

  Widget _buildMonthlyChart() {
    List<String> months = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun',
      'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];

    return Container(
      height: 250,
      padding: const EdgeInsets.all(16),
      decoration: AppTheme.cardDecoration,
      child: BarChart(
        BarChartData(
          alignment: BarChartAlignment.spaceAround,
          maxY: (_monthlyDistribution.values.isEmpty
              ? 20
              : _monthlyDistribution.values.reduce((a, b) => a > b ? a : b) * 1.2),
          barGroups: List.generate(12, (index) {
            return BarChartGroupData(
              x: index,
              barRods: [
                BarChartRodData(
                  toY: (_monthlyDistribution[index + 1] ?? 0).toDouble(),
                  color: Colors.orange,
                  width: 16,
                  borderRadius: BorderRadius.circular(4),
                ),
              ],
            );
          }),
          titlesData: FlTitlesData(
            leftTitles: AxisTitles(
              sideTitles: SideTitles(
                showTitles: true,
                reservedSize: 40,
                getTitlesWidget: (value, meta) {
                  return Text(value.toInt().toString(), style: const TextStyle(fontSize: 12));
                },
              ),
            ),
            bottomTitles: AxisTitles(
              sideTitles: SideTitles(
                showTitles: true,
                getTitlesWidget: (value, meta) {
                  if (value.toInt() >= 0 && value.toInt() < 12) {
                    return Text(months[value.toInt()], style: const TextStyle(fontSize: 10));
                  }
                  return const Text('');
                },
              ),
            ),
            topTitles: const AxisTitles(sideTitles: SideTitles(showTitles: false)),
            rightTitles: const AxisTitles(sideTitles: SideTitles(showTitles: false)),
          ),
          gridData: FlGridData(show: true, drawVerticalLine: false, horizontalInterval: 5),
          borderData: FlBorderData(show: false),
        ),
      ),
    );
  }

  Widget _buildEnergyChart() {
    List<Color> colors = [Colors.green, Colors.yellow[700]!, Colors.orange, Colors.red];
    int index = 0;
    List<BarChartGroupData> bars = [];
    _energyDistribution.forEach((key, value) {
      bars.add(BarChartGroupData(x: index, barRods: [
        BarChartRodData(toY: value.toDouble(), color: colors[index], width: 40, borderRadius: BorderRadius.circular(8)),
      ]));
      index++;
    });

    return Container(
      height: 250,
      padding: const EdgeInsets.all(16),
      decoration: AppTheme.cardDecoration,
      child: BarChart(
        BarChartData(
          alignment: BarChartAlignment.spaceAround,
          maxY: (_energyDistribution.values.isEmpty ? 100 : _energyDistribution.values.reduce((a, b) => a > b ? a : b) * 1.2),
          barGroups: bars,
          titlesData: FlTitlesData(
            leftTitles: AxisTitles(sideTitles: SideTitles(showTitles: true, reservedSize: 40)),
            bottomTitles: AxisTitles(sideTitles: SideTitles(showTitles: true, getTitlesWidget: (value, meta) {
              List<String> labels = ['Low', 'Medium', 'High', 'V.High'];
              if (value.toInt() >= 0 && value.toInt() < labels.length) {
                return Padding(padding: const EdgeInsets.only(top: 8), child: Text(labels[value.toInt()], style: const TextStyle(fontSize: 11)));
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

  Widget _buildSummaryCards() {
    int withEnergy = _fireballs.where((f) => f.totalImpactEnergyKt != null).length;
    double avgEnergy = withEnergy > 0
        ? _fireballs.where((f) => f.totalImpactEnergyKt != null).map((f) => f.totalImpactEnergyKt!).reduce((a, b) => a + b) / withEnergy
        : 0;

    return Row(
      children: [
        Expanded(
          child: Container(
            padding: const EdgeInsets.all(16),
            decoration: AppTheme.cardDecoration,
            child: Column(
              children: [
                const Icon(Icons.calculate, size: 32, color: Colors.blue),
                const SizedBox(height: 8),
                Text(avgEnergy.toStringAsFixed(2), style: const TextStyle(fontSize: 24, fontWeight: FontWeight.bold, color: Colors.blue)),
                const Text('Avg Energy (kt)', style: TextStyle(fontSize: 12, color: Colors.grey)),
              ],
            ),
          ),
        ),
        const SizedBox(width: 16),
        Expanded(
          child: Container(
            padding: const EdgeInsets.all(16),
            decoration: AppTheme.cardDecoration,
            child: Column(
              children: [
                const Icon(Icons.bolt, size: 32, color: Colors.orange),
                const SizedBox(height: 8),
                Text('$withEnergy', style: const TextStyle(fontSize: 24, fontWeight: FontWeight.bold, color: Colors.orange)),
                const Text('With Energy Data', style: TextStyle(fontSize: 12, color: Colors.grey)),
              ],
            ),
          ),
        ),
      ],
    );
  }

  Widget _buildFireballCard(Fireball fireball, int? rank) {
    final date = fireball.eventDate;
    final energy = fireball.totalImpactEnergyKt;
    final velocity = fireball.velocityKmS;

    return Container(
      margin: const EdgeInsets.only(bottom: 16),
      decoration: AppTheme.cardDecoration,
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Row(
          children: [
            if (rank != null)
              Container(
                width: 40,
                height: 40,
                decoration: BoxDecoration(gradient: const LinearGradient(colors: [Color(0xFFFF6F00), Color(0xFFFF8F00)]), borderRadius: BorderRadius.circular(10)),
                child: Center(child: Text('#$rank', style: const TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 16))),
              ),
            if (rank != null) const SizedBox(width: 12),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(DateFormat('MMM dd, yyyy - HH:mm').format(date), style: const TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
                  const SizedBox(height: 8),
                  if (energy != null)
                    Row(children: [
                      const Icon(Icons.bolt, size: 16, color: Colors.orange),
                      const SizedBox(width: 4),
                      Text('Energy: ${energy.toStringAsFixed(2)} kt', style: const TextStyle(fontSize: 14)),
                    ]),
                  if (velocity != null)
                    Row(children: [
                      const Icon(Icons.speed, size: 16, color: Colors.blue),
                      const SizedBox(width: 4),
                      Text('Velocity: ${velocity.toStringAsFixed(1)} km/s', style: const TextStyle(fontSize: 14)),
                    ]),
                  if (fireball.hasLocation)
                    Row(children: [
                      const Icon(Icons.location_on, size: 16, color: Colors.green),
                      const SizedBox(width: 4),
                      Text('Lat: ${fireball.latitude!.toStringAsFixed(2)}, Lon: ${fireball.longitude!.toStringAsFixed(2)}', style: const TextStyle(fontSize: 14)),
                    ]),
                ],
              ),
            ),
            if (energy != null && energy > 1.0)
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                decoration: BoxDecoration(color: Colors.red, borderRadius: BorderRadius.circular(8)),
                child: const Text('HIGH', style: TextStyle(color: Colors.white, fontSize: 10, fontWeight: FontWeight.bold)),
              ),
          ],
        ),
      ),
    );
  }
}