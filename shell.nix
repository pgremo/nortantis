{ pkgs ? import <nixpkgs> {}
}:let

    jdk = pkgs.jdk11;

in pkgs.mkShell {

  buildInputs = with pkgs; [
    git
    jdk
    maven
  ];

}