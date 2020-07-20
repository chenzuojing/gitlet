package gitlet;

import java.io.File;

public class Head {
    /**
     * Set up the global HEAD, default to master
     */
    public void setGlobalHEAD(String branchName, Commit commit) {
        Branch branch = new Branch(branchName, commit);
        File branchFile = Utils.join(Main.GITLET_FOLDER, "HEAD");
        Utils.writeObject(branchFile, branch);
    }

    /**
     * Return the commit node that the global HEAD pointer points to.
     */
    public static Commit getGlobalHEAD() {
        File HEAD = Utils.join(Main.GITLET_FOLDER, "HEAD");
        return Branch.load(HEAD).getHEAD();
    }

    /**
     * Update the HEAD pointer of a branch by writing the last
     * commit node into a byte array.
     */
    public void setBranchHEAD(String branchName, Commit commit) {
        Branch branch = new Branch(branchName, commit);
        File branchFile = Utils.join(Main.HEADS_REFS_FOLDER, branchName);
        Utils.writeObject(branchFile, branch);
    }

    /**
     * Return the commit node that the branch HEAD pointer points to.
     */
    public static Commit getBranchHEAD(String branchName) {
        File branch = Utils.join(Main.HEADS_REFS_FOLDER, branchName);
        return Branch.load(branch).getHEAD();
    }
}
