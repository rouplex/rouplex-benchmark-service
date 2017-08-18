import React from 'react';

var Line = require("react-chartjs-2").Line;

export default class BenchmarkGraphs extends React.Component {
  // this.props.connectParams
  // this.props.transferParams

  buildBenchmarkExpectationData() {
    this.dataSets = [];
    var cp = this.props.connectParams;
    var tp = this.props.transferParams;

    if (!cp) {
      return;
    }

    // last value not reached (exclusive for these two params)
    var maxClientLifeMillis = cp.maxClientLifeMillis - 1;
    var maxDelayMillisBeforeCreatingClient = cp.maxDelayMillisBeforeCreatingClient - 1;

    var connectsMillis = cp.maxDelayMillisBeforeCreatingClient - cp.minDelayMillisBeforeCreatingClient;
    var avgClientLifetimeMillis = (cp.minClientLifeMillis + maxClientLifeMillis) / 2;
    var rampUpMillis = Math.min(connectsMillis, avgClientLifetimeMillis);
    var upMillis = Math.max(connectsMillis, avgClientLifetimeMillis);
    var maxSimultaneousConnections = cp.clientCount * rampUpMillis / connectsMillis;
    var benchmarkMillis = maxDelayMillisBeforeCreatingClient + maxClientLifeMillis;
    var connectsPerSec = cp.clientCount * 1000 / connectsMillis;

    var startDisconnectsMillis1 = cp.minDelayMillisBeforeCreatingClient + cp.minClientLifeMillis;
    var startDisconnectsMillis2 = cp.minDelayMillisBeforeCreatingClient + maxClientLifeMillis;
    var finishDisconnectsMillis1 = maxDelayMillisBeforeCreatingClient + cp.minClientLifeMillis;
    var finishDisconnectsMillis2 = maxDelayMillisBeforeCreatingClient + maxClientLifeMillis;
    if (startDisconnectsMillis2 > finishDisconnectsMillis1) {
      var swap = finishDisconnectsMillis1;
      finishDisconnectsMillis1 = startDisconnectsMillis2;
      startDisconnectsMillis2 = swap;
    }

    this.buildConnectsGraph(cp.minDelayMillisBeforeCreatingClient, maxDelayMillisBeforeCreatingClient, connectsPerSec, benchmarkMillis);
    this.buildDisconnectsGraph(startDisconnectsMillis1, startDisconnectsMillis2, finishDisconnectsMillis1, finishDisconnectsMillis2, connectsPerSec, benchmarkMillis);
    this.buildOngoingGraph(cp.minDelayMillisBeforeCreatingClient, rampUpMillis, upMillis, finishDisconnectsMillis2, maxSimultaneousConnections, benchmarkMillis);

    if (!tp) {
      return;
    }

    // last value not reached (exclusive for these two params)
    var maxPayloadSize = tp.maxPayloadSize - 1;
    var maxDelayMillisBetweenSends = tp.maxDelayMillisBetweenSends - 1;

    var avgPayloadSize = (maxPayloadSize + tp.minPayloadSize) / 2;
    var avgPayloadPeriodMillis = (maxDelayMillisBetweenSends + tp.minDelayMillisBetweenSends) / 2;
    var maxTransferSpeedBytesPerSec = avgPayloadSize * maxSimultaneousConnections
      / avgPayloadPeriodMillis * 1000 /* leave * 1000 at the end to avoid overflow */;

    // todo build transfer graph
  }

  buildConnectsGraph(startConnectsMillis, finishConnectsMillis, connectsPerSec, benchmarkMillis) {
    this.dataSets.push({
      label: 'Connect-Rate',
      yAxisID: 'y-axis-rate',
      pointRadius: 0,
      lineTension: 0,
      borderWidth: 1,
      borderDash: [5, 10],
      borderColor: 'rgba(28,211,162,1)',
      fill: true,
      backgroundColor: 'rgba(28,211,162,0.25)',
      data: [
        {x: 0, y: 0},
        {x: startConnectsMillis, y: 0},
        {x: startConnectsMillis, y: connectsPerSec},
        {x: finishConnectsMillis, y: connectsPerSec},
        {x: finishConnectsMillis, y: 0},
        {x: benchmarkMillis, y: 0}
      ]
    });
  }

  buildDisconnectsGraph(startDisconnectsMillis1, startDisconnectsMillis2, finishDisconnectsMillis1, finishDisconnectsMillis2, disconnectsPerSec, benchmarkMillis) {
    this.dataSets.push({
      label: 'Disconnect-Rate',
      yAxisID: 'y-axis-rate',
      pointRadius: 0,
      lineTension: 0,
      borderWidth: 1,
      borderDash: [5, 10],
      borderColor: 'rgba(253,124,110,1)',
      fill: true,
      backgroundColor: 'rgba(253,124,110,0.25)',
      data: [
        {x: 0, y: 0},
        {x: startDisconnectsMillis1, y: 0},
        {x: startDisconnectsMillis2, y: disconnectsPerSec},
        {x: finishDisconnectsMillis1, y: disconnectsPerSec},
        {x: finishDisconnectsMillis2, y: 0},
        {x: benchmarkMillis, y: 0}
      ]
    });
  }

  buildOngoingGraph(startConnectsMillis, rampUpMillis, upMillis, finishDisconnectsMillis2, maxSimultaneousConnections, benchmarkMillis) {
    this.dataSets.push({
      label: 'Active-Session-Count',
      yAxisID: 'y-axis-live',
      pointRadius: 0,
      lineTension: 0,
      borderWidth: 1,
      borderColor: 'rgba(93,118,203,1)',
      fill: true,
      backgroundColor: 'rgba(93,118,203,0.25)',
      data: [
        {x: 0, y: 0},
        {x: startConnectsMillis, y: 0},
        {x: startConnectsMillis + rampUpMillis, y: maxSimultaneousConnections},
        {x: startConnectsMillis + upMillis, y: maxSimultaneousConnections},
        {x: finishDisconnectsMillis2, y: 0},
        {x: benchmarkMillis, y: 0}
      ]
    });
  }

  render() {
    this.buildBenchmarkExpectationData();

    //for (var y = 0; y < 100; y += 5) {
    //  this.makeTrain(dataSets, 30, y, y);
    //}

    const data = {
      datasets: this.dataSets
    };

    var options = {
      display: true,
      scales: {
        xAxes: [{
          type: 'linear',
          id: 'x-axis-events',
          scaleLabel: {
            display: true,
            labelString: 'Time (millis)'
          }
        }],
        yAxes: [
          {
            id: 'y-axis-rate',
            position: 'left',
            scaleLabel: {
              display: true,
              labelString: 'Rate (count/sec)'
            },
            gridLines: {
              display: false
            }
          },
          {
            id: 'y-axis-live',
            position: 'right',
            scaleLabel: {
              display: true,
              labelString: 'Quantity (count)'
            },
            gridLines: {
              display: false
            }
          }
        ]
      }
    };

    if (this.dataSets.length > 0) {
      return (
        <Line data={data} options={options} width={600} height={200}/>
      );
    } else {
      return (
        <div>Chart (will show after providing params)</div>
      )
    }
  }
}

// not throwing away, may be used ...
//convertMillisUp(timeValue) {
//  var unitNames = [" Millis", " Secs", " Mins", " Hrs", " Days"];
//  var unitRatios = [1000, 60, 60, 24];
//
//  var index = 0;
//  while (index < unitNames.length && timeValue >= 10 * unitRatios[index]) {
//    timeValue /= unitRatios[index];
//    index++;
//  }
//
//  return timeValue + unitNames[index];
//}
//
//makeRectangle(x, y, w, h) {
//  if (!w) {
//    w = 20;
//  }
//  if (!h) {
//    h = 0;
//  }
//
//  return [
//    {x: x, y: y},
//    {x: x + w, y: y},
//    {x: x + w, y: y + h},
//    {x: x, y: y + h},
//    {x: x, y: y}
//  ]
//}
//
//makeTrain(ds, n, x, y, w, h) {
//  for (var i = 0; i < n; i++, x += 30) {
//    ds.push({
//      label: 'sdf',
//      fill: false,
//      lineTension: 0,
//      backgroundColor: 'rgba(75,192,192,1)',
//      pointBorderColor: 'rgba(75,192,192,1)',
//      pointBackgroundColor: 'rgba(75,192,192,1)',
//      pointBorderWidth: 1,
//      pointRadius: 0,
//      pointHitRadius: 10,
//      data: this.makeRectangle(x, y, w, h),
//      xAxisID: 'x-axis-2',
//      yAxisID: 'y-axis-2'
//    });
//  }
//}
