package org.omancode.r.ui;

import java.awt.Dimension;
import java.awt.Font;
import java.beans.IntrospectionException;
import java.util.Collection;
import java.util.LinkedList;

import javax.swing.JFrame;

import org.omancode.r.RFace;
import org.omancode.r.RFaceException;
import org.omancode.r.types.RVectorList;

/**
 * Simple example demonstrating the {@link RSwingConsole}.
 * 
 * @author Oliver Mannion
 * @version $Revision$
 */
public class RSwingConsoleExample {

	private RFace rInterface;
	private RSwingConsole rConsole;
	private final Collection<Person> people1 = new LinkedList<Person>();

	/**
	 * Default constructor.
	 */
	public RSwingConsoleExample() {
		Person p1 = new Person(1, "mike", 'm', 18.25, true);
		Person p2 = new Person(2, "michael", 'm', 28.25, true);
		Person p3 = new Person(3, "peter", 'm', 28.25 + (1.0 / 3.0), false);
		Person p4 = new Person(4, "bob", 'm', 17, true);
		Person p5 = new Person(5, "barbara", 'f', 18.7635120384, false);

		p1.setdArray(new double[] { 1, 2, 3 });

		people1.add(p1);
		people1.add(p2);
		people1.add(p3);
		people1.add(p4);
		people1.add(p5);
	}

	public static void main(String[] args) {
		new RSwingConsoleExample().testRSwingConsole();
	}

	public void testRSwingConsole() {

		try {
			loadConsole();

			// assign collection to the R object p1
			rInterface.assignDataFrame("p1", new RVectorList(people1,
					Object.class).asRList());
			rInterface.printlnToConsole("Created p1");

			rConsole.printPrompt();

		} catch (RFaceException e) {
			e.printStackTrace();
		} catch (IntrospectionException e) {
			e.printStackTrace();
		}
	}

	public void loadConsole() throws RFaceException {
		// create console but don't show the prompt
		rConsole = new RSwingConsole(false);
		rConsole.setFont(new Font("Consolas", Font.PLAIN, 12));
		rConsole.setPreferredSize(new Dimension(500, 500));

		// load R
		try {
			rInterface = RFace.getInstance(rConsole);
		} catch (RFaceException e) {
			System.err.println(e.getMessage());
			System.err.println("Check: ");
			System.err
					.println("1) the location of jri.dll is specified, eg: -Djava.library.path=\"C:\\Program Files\\R\\R-2.11.1\\library\\rJava\\jri\"");
			System.err
					.println("2) R bin dir is on the path, eg: PATH=%PATH%;C:\\Program Files\\R\\R-2.11.1\\bin");

			System.exit(-1);
		}

		// load packages
		rInterface.loadPackage("rJava");
		rInterface.loadPackage("JavaGD");

		// Create and set up the window.
		JFrame frame = new JFrame(this.getClass().getSimpleName());
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		frame.add(rConsole);

		// Display the window.
		frame.pack();
		frame.setVisible(true);
	}

	public static class Person {

		private int id;
		private String name;
		private char gender;
		private double age;
		private boolean updated;
		private double[] dArray;

		public Person(int id, String name, char gender, double age,
				boolean updated) {
			super();
			this.id = id;
			this.name = name;
			this.gender = gender;
			this.age = age;
			this.updated = updated;
		}

		public double[] getdArray() {
			return dArray;
		}

		public void setdArray(double[] dArray) {
			this.dArray = dArray;
		}

		public int getId() {
			return id;
		}

		public void setId(int id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public char getGender() {
			return gender;
		}

		public void setGender(char gender) {
			this.gender = gender;
		}

		public double getAge() {
			return age;
		}

		public void setAge(double age) {
			this.age = age;
		}

		public boolean isUpdated() {
			return updated;
		}

		public void setUpdated(boolean updated) {
			this.updated = updated;
		}

		@Override
		public String toString() {
			StringBuffer sbuf = new StringBuffer();

			sbuf.append(age).append(", ");
			sbuf.append(gender).append(", ");
			sbuf.append(id).append(", ");
			sbuf.append(name).append(", ");
			sbuf.append(updated).append("\n");

			return sbuf.toString();
		}

		public static String collToString(Collection<?> coll) {
			StringBuffer sbuf = new StringBuffer();

			for (Object obj : coll) {
				sbuf.append(obj.toString());
			}

			return sbuf.toString();
		}

	}

}
