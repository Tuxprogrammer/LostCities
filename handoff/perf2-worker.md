# Handoff: lane/perf2-worker (D-perf2-worker-1)

State: main code and tests are committed. Verification round 2 of 2 ran 38 targeted tests: 37 passed and 1 failed.

## The failure

`PlanWorkerRouteTest.routesSteppedOnTheWorkerEqualInlineRoutes` stopped at its "the worker planned every target" assert. Worker summary at that point: 441 plans, 3616 fetches, 0 deferred, 0 failed, 0 queued; longest unit 2.552 ms. The loop waited for the full 120 s deadline.

The test has 441 targets (a 21 x 21 grid with a 16-chunk stride), so the worker planned as many plans as there are targets. Yet `CachedCityPlan.isCached` still returned false for at least one target.

Last commit: the test now resubmits uncached targets once the worker's queue is empty, the same way `PlanWorkerTest` does. This is NOT verified.

## Hypotheses, in order

1. The worker's bounded queue dropped some submissions. The resubmit added in the last commit addresses this.
2. A target's plan finished, but `CachedCityPlan` (4-way set-associative) evicted it, or the worker only counted it and never stored it. To rule this out, print which targets are uncached.

The real-code tests all pass: RouteStepTest, CityLinkGraph*, DetourSearch*, PlanWorkerTest, PlanLock*, LandmarkSuperCell* and LayoutPurityTest. The open failure is in the new fixture, not the routing.

## Next

Run on slot p2e:

```
tools/remote/lc-remote.sh build p2e HEAD -- test --tests '*PlanWorkerRoute*'
```

If it still fails, log the uncached targets.
