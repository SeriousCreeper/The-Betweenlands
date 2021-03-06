package thebetweenlands.api.rune.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableList;

import net.minecraft.util.ResourceLocation;
import thebetweenlands.api.rune.INodeBlueprint.INodeIO;
import thebetweenlands.api.rune.INodeConfiguration;

/**
 * This port based node configuration allows for easy creation of configurations
 * by using input and output ports that accepts specific types.
 */
public class PortNodeConfiguration implements INodeConfiguration {
	public static final class Builder {
		private int inIndices = 0;
		private int outIndices = 0;

		private int id = 0;

		private final List<InputPort<?>> inputPorts = new ArrayList<>();
		private final List<OutputPort<?>> outputPorts = new ArrayList<>();

		@Nullable
		private final String descriptorWildcard;

		public Builder(@Nullable ResourceLocation descriptorWildcard) {
			this.descriptorWildcard = descriptorWildcard != null ? this.getDescriptorString(descriptorWildcard) : null;
		}

		private String getDescriptorString(ResourceLocation descriptor) {
			return String.format("%s.%s", descriptor.getNamespace(), descriptor.getPath());
		}
		
		/**
		 * Creates a new input that accepts the specified type
		 * @param descriptor - descriptor that identifies the input type
		 * @param type - type to accept
		 * @return a new input that accepts the specified type
		 */
		public <T> InputPort<T> in(ResourceLocation descriptor, Class<T> type) {
			String desc = this.getDescriptorString(descriptor);
			InputPort<T> input = new InputPort<T>(type, this.inIndices++, desc.equals(this.descriptorWildcard), desc, false);
			this.inputPorts.add(input);
			return input;
		}

		/**
		 * Creates a new input that accepts any of the specified types
		 * @param descriptor - descriptor that identifies the input types
		 * @param type - type to accept
		 * @return a new input that accepts the specified type
		 */
		public InputPort<?> in(ResourceLocation descriptor, Class<?>... types) {
			String desc = this.getDescriptorString(descriptor);
			InputPort<?> input = new InputPort<>(types, this.inIndices++, desc.equals(this.descriptorWildcard), desc, false);
			this.inputPorts.add(input);
			return input;
		}

		/**
		 * Creates a new input that accepts a multiple objects of the specified type at once
		 * @param descriptor - descriptor that identifies the input type
		 * @param type - type to accept
		 * @return a new input that accepts a multiple objects of the specified type at once
		 */
		public <T> InputPort<Collection<T>> multiIn(ResourceLocation descriptor, Class<T> type) {
			String desc = this.getDescriptorString(descriptor);
			InputPort<Collection<T>> input = new InputPort<Collection<T>>(type, this.inIndices++, desc.equals(this.descriptorWildcard), desc, true);
			this.inputPorts.add(input);
			return input;
		}

		/**
		 * Creates a new output that produces the specified type
		 * @param descriptor - descriptor that identifies the output type
		 * @param type - type to produce
		 * @return a new output that produces the specified type
		 */
		public <T> OutputPort<T> out(ResourceLocation descriptor, Class<T> type) {
			OutputPort<T> output = new OutputPort<>(type, this.outIndices++, String.format("%s.%s", descriptor.getNamespace(), descriptor.getPath()));
			this.outputPorts.add(output);
			return output;
		}

		/**
		 * Creates a new output that produces the same type as the specified input
		 * @param type - upper bound of the type to produce
		 * @param in - the input whose type should be used as output type
		 * @return a new output that produces the same type as the specifeid input
		 */
		public <T> OutputPort<T> out(Class<T> type, InputPort<? extends T> in) {
			PassthroughOutputPort<T> output = new PassthroughOutputPort<>(type, this.outIndices++, in);
			this.outputPorts.add(output);
			return output;
		}

		/**
		 * Creates a new output that produces multiple objects of the specified type at once
		 * @param descriptor - descriptor that identifies the output type
		 * @param type - type to produce
		 * @return a new output that produces multiple objects of the specified type at once
		 */
		public <T> OutputPort<Collection<T>> multiOut(ResourceLocation descriptor, Class<T> type) {
			OutputPort<Collection<T>> output = new OutputPort<>(type, this.outIndices++, String.format("%s.%s", descriptor.getNamespace(), descriptor.getPath()), true);
			this.outputPorts.add(output);
			return output;
		}

		/**
		 * Creates a new output that produces multiple objects of the same type as the specified input at once
		 * @param type - upper bound of the type to produce
		 * @param in - the input whose type should be used as output type
		 * @return a new output that produces multiple objects of the same type as the specified input at once
		 */
		public <T, A extends T> OutputPort<Collection<T>> multiOut(Class<T> type, InputPort<Collection<A>> in) {
			PassthroughOutputPort<Collection<T>> output = new PassthroughOutputPort<>(type, this.outIndices++, in, true);
			this.outputPorts.add(output);
			return output;
		}

		/**
		 * Creates the new configuration
		 * @return the new configuration
		 */
		public PortNodeConfiguration build() {
			ImmutableList.Builder<IConfigurationInput> inputTypes = ImmutableList.builder();
			ImmutableList.Builder<IConfigurationOutput> outputTypes = ImmutableList.builder();

			for(InputPort<?> input : this.inputPorts) {
				if(input.type != null) {
					inputTypes.add(new IConfigurationInput() {
						@Override
						public boolean test(IConfigurationOutput output, IType type) {
							if(input.type.isAssignableFrom(type.getTypeClass()) && (input.isDescriptorWildcard() || output.getDescriptor().equals(input.getDescriptor()))) {
								//TODO Check generics?
								return true;
							}
							return false;
						}

						@Override
						public boolean isCollection() {
							return input.isMulti;
						}

						@Override
						public String getDescriptor() {
							return input.getDescriptor();
						}
					});
				} else {
					inputTypes.add(new IConfigurationInput() {
						@Override
						public boolean test(IConfigurationOutput output, IType type) {
							if(input.isDescriptorWildcard() || output.getDescriptor().equals(input.getDescriptor())) {
								for(Class<?> inputType : input.types) {
									if(inputType.isAssignableFrom(type.getTypeClass())) {
										//TODO Check generics?
										return true;
									}
								}
							}
							return false;
						}

						@Override
						public boolean isCollection() {
							return input.isMulti;
						}

						@Override
						public String getDescriptor() {
							return input.getDescriptor();
						}
					});
				}
			}

			for(OutputPort<?> output : this.outputPorts) {
				final IType type = new IType() {
					@Override
					public Class<?> getTypeClass() {
						return output.type;
					}

					@Override
					public List<IType> getTypeGenerics() {
						return Collections.emptyList(); //TODO Add generics to output ports?
					}
				};
				outputTypes.add(new IConfigurationOutput() {
					@Override
					public IType getType(List<IType> inputs) {
						if(output instanceof PassthroughOutputPort) {
							PassthroughOutputPort<?> passthrough = (PassthroughOutputPort<?>) output;
							return inputs.get(passthrough.input.index);
						}
						return type;
					}

					@Override
					public boolean isEnabled(List<IType> inputs) {
						if(output instanceof PassthroughOutputPort) {
							PassthroughOutputPort<?> passthrough = (PassthroughOutputPort<?>) output;
							return inputs.get(passthrough.input.index) != null;
						}
						return true;
					}

					@Override
					public boolean isCollection() {
						return output.isMulti;
					}

					@Override
					public String getDescriptor() {
						return output.getDescriptor();
					}
				});
			}

			this.inIndices = 0;
			this.outIndices = 0;
			this.inputPorts.clear();
			this.outputPorts.clear();

			return new PortNodeConfiguration(inputTypes.build(), outputTypes.build(), this.id++);
		}
	}

	/**
	 * An input port that allows retrieving values from the node input
	 */
	public static class InputPort<T> {
		private final Class<T> type;
		private final Class<?>[] types;
		private final int index;
		private final boolean isMulti;
		private final boolean descriptorWildcard;
		private final String descriptor;

		private InputPort(Class<T> type, int index, boolean descriptorWildcard, String descriptor) {
			this.type = type;
			this.types = null;
			this.index = index;
			this.isMulti = false;
			this.descriptorWildcard = descriptorWildcard;
			this.descriptor = descriptor;
		}

		@SuppressWarnings("unchecked")
		private InputPort(Class<?> type, int index, boolean descriptorWildcard, String descriptor, boolean isMulti) {
			this.type = (Class<T>) type;
			this.types = null;
			this.index = index;
			this.isMulti = isMulti;
			this.descriptorWildcard = descriptorWildcard;
			this.descriptor = descriptor;
		}

		private InputPort(Class<?>[] types, int index, boolean descriptorWildcard, String descriptor) {
			this.type = null;
			this.types = types;
			this.index = index;
			this.isMulti = false;
			this.descriptorWildcard = descriptorWildcard;
			this.descriptor = descriptor;
		}

		private InputPort(Class<?>[] types, int index, boolean descriptorWildcard, String descriptor, boolean isMulti) {
			this.type = null;
			this.types = types;
			this.index = index;
			this.isMulti = isMulti;
			this.descriptorWildcard = descriptorWildcard;
			this.descriptor = descriptor;
		}

		public boolean isDescriptorWildcard() {
			return this.descriptorWildcard;
		}

		public String getDescriptor() {
			return this.descriptor;
		}

		/**
		 * Returns the input value at this port, without doing any type checks
		 * @param input - node I/O
		 * @return the input value at this port
		 */
		@SuppressWarnings("unchecked")
		public T get(INodeIO io) {
			// TODO Type check
			return (T) io.get(this.index);
		}

		/**
		 * Returns the input value at this port after doing a type check.
		 * If the object cannot be cast to the specified type <code>null</code> is returned.
		 * @param io node I/O
		 * @param cls type to cast to
		 * @return the input value at this port, or null if the input value cannot be cast to the specified type
		 */
		@SuppressWarnings("unchecked")
		public <F> F get(INodeIO io, Class<F> cls) {
			Object obj = io.get(this.index);
			if(obj == null || cls.isInstance(obj)) {
				return (F) obj;
			}
			return null;
		}

		/**
		 * Returns the input value at this port after doing a type check.
		 * If the object cannot be cast to the specified type <i>defaultValue</i> is returned.
		 * @param io node I/O
		 * @param cls type to cast to
		 * @param defaultValue default value returned if input value cannot be cast to the specified type
		 * @return the input value at this port, or <i>defaultValue</i> if the input value cannot be cast to the specified type
		 */
		@SuppressWarnings("unchecked")
		public <F> F get(INodeIO io, Class<F> cls, F defaultValue) {
			Object obj = io.get(this.index);
			if(obj == null || cls.isInstance(obj)) {
				return (F) obj;
			}
			return defaultValue;
		}

		/**
		 * Calls the consumer with the input value at this port after doing a type check.
		 * If the object cannot be cast to the specified type the consumer will not be called.
		 * @param io node I/O
		 * @param cls type to cast to
		 * @param runnable consumer to call with the cast input value
		 */
		@SuppressWarnings("unchecked")
		public <F> void run(INodeIO io, Class<F> cls, Consumer<F> runnable) {
			Object obj = io.get(this.index);
			if(obj == null || cls.isInstance(obj)) {
				runnable.accept((F) obj);
			}
		}
	}

	/**
	 * An output port that allows setting values of the node output
	 */
	public static class OutputPort<T> {
		private final Class<T> type;
		private final int index;
		private final boolean isMulti;
		private final String descriptor;

		private OutputPort(Class<T> type, int index, String descriptor) {
			this.type = type;
			this.index = index;
			this.isMulti = false;
			this.descriptor = descriptor;
		}

		@SuppressWarnings("unchecked")
		private OutputPort(Class<?> type, int index, String descriptor, boolean isMulti) {
			this.type = (Class<T>) type;
			this.index = index;
			this.isMulti = isMulti;
			this.descriptor = descriptor;
		}

		public String getDescriptor() {
			return this.descriptor;
		}

		/**
		 * Sets the output value at this port
		 * @param io - node I/O
		 * @param obj - value to output
		 */
		public void set(INodeIO io, T obj) {
			io.set(this.index, obj);
		}
	}

	private static class PassthroughOutputPort<T> extends OutputPort<T> {
		private final InputPort<?> input;

		private PassthroughOutputPort(Class<T> type, int index, InputPort<?> input) {
			super(type, index, null);
			this.input = input;
		}

		private PassthroughOutputPort(Class<?> type, int index, InputPort<?> input, boolean isMulti) {
			super(type, index, null, isMulti);
			this.input = input;
		}

		@Override
		public String getDescriptor() {
			return this.input.getDescriptor();
		}
	}

	private final List<IConfigurationInput> inputTypes;
	private final List<IConfigurationOutput> outputTypes;
	private final int id;

	private PortNodeConfiguration(List<IConfigurationInput> inputTypes, List<IConfigurationOutput> outputTypes, int id) {
		this.inputTypes = inputTypes;
		this.outputTypes = outputTypes;
		this.id = id;
	}

	public static Builder builder() {
		return new Builder(null);
	}

	/**
	 * Creates a port node configuration builder with the specified descriptor as input type descriptor wildcard. 
	 * When an input of this builder specifies this wildcard as type descriptor any output
	 * will be accepted regardless of the output's type descriptor as long as the input's
	 * and output's Java types are compatible.
	 * @param descriptorWildcard Descriptor wildcard
	 * @return New port node configuration builder
	 */
	public static Builder builder(ResourceLocation descriptorWildcard) {
		return new Builder(descriptorWildcard);
	}

	@Override
	public List<IConfigurationInput> getInputs() {
		return this.inputTypes;
	}

	@Override
	public List<IConfigurationOutput> getOutputs() {
		return this.outputTypes;
	}

	@Override
	public int getId() {
		return this.id;
	}
}
