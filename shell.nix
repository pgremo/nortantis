{ pkgs ? import <nixpkgs> {}
}:let

in pkgs.mkShell {

  buildInputs = with pkgs; [
    git
    jdk
    maven
  ];

}