import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class HeapSort<T> {
  List<T> data;

  public void init(List<T> list) {
    data = new ArrayList<>();
    data.addAll(list);
  }

  /*
   * Given a list and a comparator, sort the list using heap sort.
   * 
   * @param list the list to sort
   * 
   * @param comp the comparator
   * 
   * @return the sorted list
   */
  public ArrayList<T> heapsort(List<T> list, Comparator<T> comp) {
    int size = list.size();

    for (int i = (size - 1) / 2; i > -1; i--) {
      this.downheap(list, comp, i);
    }

    ArrayList<T> ans = new ArrayList<T>();
    for (int i = 0; i < size; i++) {
      ans.add(this.removeMax(list, comp));
    }
    return ans;
  }

  // downheaps once on the given list from index i
  public void downheap(List<T> list, Comparator<T> comp, int i) {
    int leftIndex = 2 * i + 1;
    int rightIndex = 2 * i + 2;

    if (leftIndex < list.size() && rightIndex < list.size()) {
      T curr = list.get(i);
      T leftChild = list.get(leftIndex);
      T rightChild = list.get(rightIndex);

      if (comp.compare(curr, leftChild) < 0 || comp.compare(curr, rightChild) < 0) {
        if (comp.compare(leftChild, rightChild) >= 0) {
          this.swap(list, i, leftIndex);
          this.downheap(list, comp, leftIndex);
        }
        else {
          this.swap(list, i, rightIndex);
          this.downheap(list, comp, rightIndex);
        }
      }
    }
    else if (2 * i + 1 < list.size()) {
      T curr = list.get(i);
      T child = list.get(leftIndex);

      if (comp.compare(curr, child) < 0) {
        this.swap(list, i, leftIndex);
        this.downheap(list, comp, leftIndex);
      }
    }
  }

  // removes the max from the given list using the given comparator
  public T removeMax(List<T> list, Comparator<T> comp) {
    if (list.size() > 0) {
      T max = list.get(0);
      this.swap(list, 0, list.size() - 1);
      list.remove(list.size() - 1);

      for (int i = (list.size() - 1) / 2; i > -1; i--) {
        this.downheap(list, comp, i);
      }

      return max;
    }
    else {
      throw new IllegalArgumentException("Can't remove from nothing.");
    }
  }
  
  // swaps two elements in a list if both are in range
  public void swap(List<T> list, int i1, int i2) {
    if (i1 < list.size() && i2 < list.size()) {
      T val1 = list.get(i1);
      T val2 = list.get(i2);
      
      list.set(i1, val2);
      list.set(i2, val1);
    }
  }
}

class IntComparator implements Comparator<Integer> {

  // compares the two integers
  // 0 - integers are the same
  // < 0 - second integer is larger
  // > 0 - first integer is larger
  public int compare(Integer int1, Integer int2) {
    return int1 - int2;
  }

}