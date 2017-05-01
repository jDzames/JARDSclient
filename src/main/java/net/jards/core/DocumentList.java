package net.jards.core;

import java.util.*;
import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

/**
 * Ordered list of documents. Uses ArrayList inside, just simplifies work.
 */
public class DocumentList extends AbstractList<Document> {

    private List<Document> documents = new ArrayList<>();

    DocumentList(List<Document> documents){
        this.documents = documents;
    }

    @Override
    public boolean add(Document document) {
        return documents.add(document);
    }

    @Override
    public void add(int index, Document element) {
        documents.add(index, element);
    }

    @Override
    public boolean addAll(Collection<? extends Document> c) {
        return documents.addAll(c);
    }

    @Override
    public boolean addAll(int index, Collection<? extends Document> c) {
        return documents.addAll(index, c);
    }

    @Override
    public Document get(int index) {
        return documents.get(index);
    }


    @Override
    public Document set(int index, Document element) {
        return documents.set(index, element);
    }

    @Override
    public boolean remove(Object o) {
        return documents.remove(o);
    }

    @Override
    public Document remove(int index) {
        return documents.remove(index);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return documents.removeAll(c);
    }

    @Override
    public boolean removeIf(Predicate<? super Document> filter) {
        return documents.removeIf(filter);
    }

    @Override
    public boolean contains(Object o) {
        return documents.contains(o);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return documents.containsAll(c);
    }

    @Override
    public boolean equals(Object o) {
        return documents.equals(o);
    }

    @Override
    public boolean isEmpty() {
        return documents.isEmpty();
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return documents.retainAll(c);
    }

    @Override
    public int hashCode() {
        return documents.hashCode();
    }

    @Override
    public int indexOf(Object o) {
        return documents.indexOf(o);
    }

    @Override
    public int lastIndexOf(Object o) {
        return documents.lastIndexOf(o);
    }

    @Override
    public Iterator<Document> iterator() {
        return documents.iterator();
    }

    @Override
    public List<Document> subList(int fromIndex, int toIndex) {
        return documents.subList(fromIndex, toIndex);
    }

    @Override
    public ListIterator<Document> listIterator() {
        return documents.listIterator();
    }

    @Override
    public Object[] toArray() {
        return documents.toArray();
    }

    @Override
    public Spliterator<Document> spliterator() {
        return documents.spliterator();
    }

    @Override
    public Stream<Document> parallelStream() {
        return documents.parallelStream();
    }

    @Override
    public Stream<Document> stream() {
        return documents.stream();
    }

    @Override
    public String toString() {
        return documents.toString();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return documents.toArray(a);
    }

    @Override
    public void clear() {
        documents.clear();
    }

    @Override
    public ListIterator<Document> listIterator(int index) {
        return documents.listIterator(index);
    }

    @Override
    public void replaceAll(UnaryOperator<Document> operator) {
        documents.replaceAll(operator);
    }

    @Override
    public void sort(Comparator<? super Document> c) {
        documents.sort(c);
    }

    @Override
    public void forEach(Consumer<? super Document> action) {
        documents.forEach(action);
    }


    @Override
	public int size() {
		return documents.size();
	}


}


/*
* doplnit ako cursor
* list
*
* */