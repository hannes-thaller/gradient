import time

import attr


@attr.s(slots=True, repr=False)
class Timer:
    start = attr.ib(factory=time.time, init=False)

    def __enter__(self):
        self.start = time.time()
        return self

    def __exit__(self, exc_type, exc_val, exc_tb):
        pass

    @property
    def duration(self):
        return time.time() - self.start

    def reset(self):
        self.start = time.time()

    def __repr__(self):
        return f"[{self.duration:.2f}s]"
