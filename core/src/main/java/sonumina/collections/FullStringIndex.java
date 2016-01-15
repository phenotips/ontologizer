package sonumina.collections;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * A class mapping strings to other objects. The data structure compares the strings in a case-insensitive manner.
 *
 * @author Sebastian Bauer
 */
public class FullStringIndex<T>
{
    private ArrayList<String> stringList = new ArrayList<String>();

    private ArrayList<T> objectList = new ArrayList<T>();

    /**
     * Private class to iterate over strings containing a given string.
     *
     * @author Sebastian Bauer
     */
    private class StringIterator implements Iterator<T>
    {
        private String str;

        private int pos = -1;

        public StringIterator(String str)
        {
            this.str = str;
        }

        @Override
        public boolean hasNext()
        {
            while (true) {
                this.pos++;
                if (this.pos >= FullStringIndex.this.stringList.size()) {
                    return false;
                }
                if (FullStringIndex.this.stringList.get(this.pos).contains(this.str)) {
                    return true;
                }
            }
        }

        @Override
        public T next()
        {
            return FullStringIndex.this.objectList.get(this.pos);
        }

        @Override
        public void remove()
        {
        }
    }

    /**
     * Associates the given string with the given object.
     *
     * @param string
     * @param t
     */
    public void add(String string, T o)
    {
        this.stringList.add(string.toLowerCase());
        this.objectList.add(o);
    }

    /**
     * Clears the container.
     */
    public void clear()
    {
        this.stringList.clear();
        this.objectList.clear();
    }

    /**
     * Returns the size of the index, i.e., the total number of strings.
     *
     * @return
     */
    public int size()
    {
        return this.stringList.size();
    }

    /**
     * Returns an iterable which can be used to iterate over elements that contain the given string.
     *
     * @param string
     * @return
     */
    public Iterable<T> contains(final String string)
    {
        return new Iterable<T>()
        {
            @Override
            public Iterator<T> iterator()
            {
                return new StringIterator(string.toLowerCase());
            }
        };
    }
}
