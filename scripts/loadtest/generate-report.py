import csv
import json
from pathlib import Path

import matplotlib

matplotlib.use("Agg")
import matplotlib.pyplot as plt


ROOT = Path(__file__).resolve().parents[2]
RESULTS_DIR = ROOT / "docs" / "loadtest-results"

CASES = [
    ("baseline", "read", RESULTS_DIR / "baseline-read.json"),
    ("baseline", "write", RESULTS_DIR / "baseline-write.json"),
    ("baseline", "search", RESULTS_DIR / "baseline-search.json"),
    ("limited", "read", RESULTS_DIR / "limited-read.json"),
    ("limited", "write", RESULTS_DIR / "limited-write.json"),
    ("limited", "search", RESULTS_DIR / "limited-search.json"),
]


def load_metrics(path: Path) -> tuple[float, float]:
    payload = json.loads(path.read_text())
    rps = float(payload["metrics"]["http_reqs"]["rate"])
    p95 = float(payload["metrics"]["http_req_duration"]["p(95)"])
    return rps, p95


def main() -> None:
    results = []
    for env, scenario, path in CASES:
        if path.exists() is False:
            raise SystemExit(f"Missing summary file: {path}")
        rps, p95 = load_metrics(path)
        results.append(
            {
                "env": env,
                "scenario": scenario,
                "rps": rps,
                "p95_ms": p95,
            }
        )

    summary_csv = RESULTS_DIR / "summary.csv"
    with summary_csv.open("w", newline="", encoding="utf-8") as handle:
        writer = csv.DictWriter(handle, fieldnames=["env", "scenario", "rps", "p95_ms"])
        writer.writeheader()
        writer.writerows(results)

    scenarios = ["read", "write", "search"]
    envs = ["baseline", "limited"]
    rps_map = {(r["env"], r["scenario"]): r["rps"] for r in results}
    p95_map = {(r["env"], r["scenario"]): r["p95_ms"] for r in results}

    def plot_metric(metric_map, title, ylabel, filename):
        x = range(len(scenarios))
        width = 0.35
        fig, ax = plt.subplots(figsize=(8, 4.5))
        for idx, env in enumerate(envs):
            values = [metric_map[(env, scenario)] for scenario in scenarios]
            offset = (idx - 0.5) * width
            ax.bar([i + offset for i in x], values, width, label=env)

        ax.set_xticks(list(x))
        ax.set_xticklabels([s.capitalize() for s in scenarios])
        ax.set_title(title)
        ax.set_ylabel(ylabel)
        ax.legend()
        ax.grid(axis="y", linestyle="--", alpha=0.4)
        fig.tight_layout()
        fig.savefig(RESULTS_DIR / filename, dpi=160)
        plt.close(fig)

    plot_metric(rps_map, "Throughput (requests/sec)", "requests/sec", "throughput-rps.png")
    plot_metric(p95_map, "Latency p95 (ms)", "ms", "latency-p95.png")


if __name__ == "__main__":
    main()
