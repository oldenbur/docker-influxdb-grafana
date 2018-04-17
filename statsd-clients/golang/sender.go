package main

import (
	"github.com/cihub/seelog"
	"io"
	"math"
	"math/rand"
	"sync"
	"time"
)

const (
	clusterId   = "tig-cluster"
	serviceName = "golang-statsd"
	countField  = "gocount"
	timingField = "gotiming"
	gaugeField  = "gogauge"
)

type StatsSender interface {
	io.Closer
	Send() error
}

type statsSender struct {
	statsdAddr string
	closeChan  chan bool
	stats      StatsdSender
	wg         *sync.WaitGroup
}

func NewStatsSender(statsdAddr string) StatsSender {
	return &statsSender{
		statsdAddr: statsdAddr,
		closeChan:  make(chan bool),
		wg:         &sync.WaitGroup{},
	}
}

func (s *statsSender) Send() error {

	seelog.Infof(`connecting to statsd at '%s'`, s.statsdAddr)
	var err error
	s.stats, err = NewStatsdSender(s.statsdAddr, clusterId, serviceName)
	if err != nil {
		return seelog.Errorf("statsdFeed statsd.New error: %v", err)
	}

	go func() {
		s.wg.Add(1)
		defer s.wg.Done()

		s.sendStats()
	}()

	return nil
}

func (s *statsSender) Close() error {

	if s.stats != nil {
		s.stats.Close()
	}
	s.closeChan <- true
	s.wg.Wait()

	return nil
}

func (s *statsSender) sendStats() {
	t := time.NewTicker(5 * time.Second)
	defer t.Stop()

	g := newGrowShrinker(0, 100)

	for {
		select {
		case <-t.C:
			seelog.Tracef(`firing stats`)

			cnt := int(math.Floor((rand.NormFloat64() / 0.1) + 50))
			s.stats.Count(countField, cnt)

			dur := math.Floor(rand.ExpFloat64() / 0.001)
			s.stats.Timing(timingField, dur)

			qlen := g.sample()
			s.stats.Gauge(gaugeField, qlen)

			seelog.Debugf(`fired stats - %s: %d  %s: %f  %s: %d`,
				countField, cnt, timingField, dur, gaugeField, qlen)

			break

		case <-s.closeChan:
			seelog.Infof(`closing stats timer`)
			return

		}
	}
}

type growShrinker struct {
	dir, val, min, max int
}

func newGrowShrinker(min, max int) *growShrinker {
	return &growShrinker{0, min + (max-min)/2, min, max}
}

func (g *growShrinker) sample() int {

	dirDelta := int(math.Floor((rand.NormFloat64() / 0.5) + .5))

	if (dirDelta < 0) && (g.val <= g.min) {
		g.val = g.min
		g.dir = 0
		return g.val
	}
	if (dirDelta > 0) && (g.val >= g.max) {
		g.val = g.max
		g.dir = 0
		return g.val
	}

	g.dir += dirDelta
	g.val += g.dir
	g.val = int(math.Min(math.Max(float64(g.val), float64(g.min)), float64(g.max)))

	return g.val
}
