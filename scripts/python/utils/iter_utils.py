class PeekIterator:
    def __init__(self, iterator):
        self.iterator = iterator
        self._peek = None


    def peek(self):
        if self._peek is None:
            self._peek = next(self.iterator, None)

        return self._peek


    def __next__(self):
        if self._peek is None:
            return next(self.iterator)

        result = self._peek
        self._peek = None
        return result


    def __iter__(self):
        return self
