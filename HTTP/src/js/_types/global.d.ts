type Extendable<T> = number | T;
type Falseable<T> = false | T;
type Nullable<T> = null | T;
type Arrayable<T> = T | T[];

type InputValidity = {
  valid: boolean;
  error?: string;
}
