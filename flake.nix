{
  description = "Clojure library for interacting with MQTT.";

  inputs = {
    nixpkgs.url = "nixpkgs/nixos-23.11";
    utils.url = "github:numtide/flake-utils";
    helpers = {
      url =
        "git+https://git.fudo.org/fudo-public/nix-helpers.git?ref=with-deps";
      inputs.nixpkgs.follows = "nixpkgs";
    };
  };

  outputs = { self, nixpkgs, utils, helpers, ... }:
    utils.lib.eachDefaultSystem (system:
      let pkgs = nixpkgs.legacyPackages."${system}";
      in {
        packages = rec {
          default = milquetoast;
          milquetoast = helpers.packages."${system}".mkClojureLib {
            name = "org.fudo/milquetoast";
            src = ./.;
            buildCommand = "clojure -T:build uberjar";
          };
        };

        devShells = rec {
          default = updateDeps;
          updateDeps = pkgs.mkShell {
            buildInputs = with helpers.packages."${system}";
              [ updateClojureDeps ];
          };
        };
      });
}
