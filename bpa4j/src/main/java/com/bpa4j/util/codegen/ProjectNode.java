package com.bpa4j.util.codegen;

/**
 * A representation of element in the project.
 * A model, synchronized with the file system (or other persistency mechanism).
 * </p>
 * Must have reading and writing constructors:
 * one (reading) accepting a {@link PhysicalNode} and reading model from it,
 * and the other (writing) accepting {@link PhysicalNode} with additional arguments and writing model to it.
 * Both should ensure the node exists (or not exists).
 * </p>
 * {@code ProjectNode} is expected to have methods with pair delegations:
 * to model and to physical node.
 * 
 * @apiNote
 * The first delegation must ALWAYS be done to the model,
 * otherwise the data may be overwriten before checks.
 */
public interface ProjectNode<T extends ProjectNode<T>>{
	/**
	 * Acts as a container for the node state.
	 * </p>
	 * Should not modify the or store the node itself:
	 * instead, it writes to or reads the node from file,
	 * and then makes any necessary changes independendly.
	 * </p>
	 * Does not make any persistent changes in constructor.
	 * All operations must be performed in the methods,
	 * while constructor determines location of future data.
	 */
	static interface PhysicalNode<V extends ProjectNode<V>>{
		/**
		 * Deletes the physical representation.
		 */
		void clear();
		/**
		 * Creates a new node physical representation.
		 * It may be a file, or a database record or any other persistency mechanism.
		 * @throws IllegalStateException if this object is already associated with physical representation.
		 */
		void persist(NodeModel<V>node)throws IllegalStateException;
		/**
		 * Loads the node from this container.
		 */
		NodeModel<V>load();
		/**
		 * Checks whether this container is "full"
		 * (there is a real physical representation).
		 */
		boolean exists();
	}
	/**
	 * Stores the node's data.
	 * Has only validation logic.
	 */
	static interface NodeModel<V extends ProjectNode<V>>{
		
	}
	PhysicalNode<T>getPhysicalRepresentation();
	NodeModel<T>getModel();
}