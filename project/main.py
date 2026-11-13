"""
main.py
Unified entry point.

Usage:
  python main.py train         # Train all models
  python main.py evaluate      # Evaluate best model
  python main.py serve         # Start FastAPI server
  python main.py predict       # Single CLI prediction (see predict.py --help)
"""

import sys
import subprocess
import os


def train():
    print("[Main] Starting training pipeline ...")
    import train as t
    t.main()


def evaluate():
    print("[Main] Starting evaluation ...")
    import evaluate as e
    e.main()


def serve():
    print("[Main] Starting FastAPI server on http://0.0.0.0:8000 ...")
    # Use the same python interpreter to run uvicorn as a module
    subprocess.run(
        [sys.executable, "-m", "uvicorn", "api.main:app", "--host", "0.0.0.0", "--port", "8000", "--reload"],
        check=True
    )


def predict_cli():
    import predict as p
    p.main()


COMMANDS = {
    "train":    train,
    "evaluate": evaluate,
    "serve":    serve,
    "predict":  predict_cli,
}

if __name__ == "__main__":
    cmd = sys.argv[1] if len(sys.argv) > 1 else "help"
    if cmd in COMMANDS:
        COMMANDS[cmd]()
    else:
        print(__doc__)
        print(f"Available commands: {list(COMMANDS.keys())}")
