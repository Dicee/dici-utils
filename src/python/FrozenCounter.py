class FrozenCounter:
    def __init__(self,iterable=None,**kwds):
        self.counter     = Counter(iterable,kwds)
        self.__dict__    = self.counter.__dict__
        self.__weakref__ = self.counter.__weakref__
        self.__hash__    = self.__hash__()
   
    def __hash__(self):
        res = 0
        for k,v in self.counter.items():
            res += 31*k.__hash__()*v.__hash__()
        return res
   
    def __add__(self,other):
        return self.counter + other.counter
   
    def __and__(self,other):
        return self.counter & other.counter
   
    def __delitem__(self,elem):
        self.counter.__delitem__(elem)
   
    def __missing__(self,key):
        return self.counter.__missing__(key)
   
    def __or__(self,other):
        return self.counter | other.counter
   
    def __reduce__(self):
        return self.counter.__reduce__()
   
    def __repr__(self):
        "Frozen" + str(self.counter)
   
    def __sub__(self,other):
        return self.counter - other.counter
   
    def copy(self):
        return FrozenCounter(self.counter)
   
    def elements(self):
        return self.counter.elements()
   
    def most_common(self,n=None):
        self.counter.most_common(n)
   
    #def fromkeys(cls,iterable,v=None):
    #    return
   
    def __contains__(self,key):
        return key in self.counter
   
    def __eq__(self,other):
        return self.counter == other.counter
   
    def __ge__(self,other):
        return self.counter >= other.counter
   
    def __getitem__(self,key):
        return self.counter[key]
   
    def __gt__(self,other):
        return self.counter > other.counter
   
    def __le__(self,other):
        return self.counter <= other.counter
   
    def __len__(self):
        return len(self.counter)
   
    def __lt__(self,other):
        return self.counter < other.counter
   
    def __ne__(self,other):
        return self.counter != other.counter
   
    def __setitem__(self,index,item):
        return self.counter.__setitem__(i,item)
   
    def __sizeof__(self):
        return self.counter.__sizeof__()
   
    def clear(self):
        self.counter.clear()
   
    def get(self,key,d=None):
        return self.counter.get(key,d)
   
    def items(self):
        return self.counter.items()
   
    def keys(self):
        return self.counter.keys()
   
    def pop(self,key,d=None):
        return self.counter.pop(key,d)
   
    def popitem(self):
        return self.counter.popitem()
   
    def setdefault(self,key,d=None):
        return self.counter.setdefault(key,d)
   
    def values(self):
        return self.counter.values()
