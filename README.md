# santa-claus

Clojure implementation of the well-known synchronization problem called "Santa Claus" with 9 reindeer, 13 elves, and of course Santa Claus himself. Since refs, agent, and atoms don't block explicitly thanks to software transactional memory, this implementation uses cora.async threads and channels.

## Installation

Download from https://github.com/oxoo2a/santa-claus-clj.

## Usage

FIXME: explanation

    $ java -jar santa-claus-0.1.0-standalone.jar [args]

## Options

No options required.

## Examples

...

### Bugs

The following issues exist:
- If there is a high frequency of elf requests, more than a maximum of 3 elves get help. There is another queue missing that throttles the elves.
- Symbols in core.async can't be refered direclty.
- Thread clean-up at program end is not working yet.
- Currently, keyword maps are used to represent abstractions such as the barrier inside the program. There might be better ways to do this.

### Any Other Sections
### That You Think
### Might be Useful

## License

Copyright © 2019 FIXME

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
